import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure

/**
 * Java version required by the given Minecraft version.
 */
fun requiredJavaFor(minecraft: String): JavaVersion {
    val parts = minecraft.split('.').map { it.toInt() }
    val (major, minor, patch) = parts + List(3 - parts.size) { 0 }
    return when {
        major >= 26 -> JavaVersion.VERSION_25
        minor > 20 || (minor == 20 && patch >= 5) -> JavaVersion.VERSION_21
        else -> JavaVersion.VERSION_17
    }
}

class PublishingDependencies {
    val required = mutableListOf<String>()
    val optional = mutableListOf<String>()

    fun requires(vararg slugs: String) { required += slugs }
    fun optional(vararg slugs: String) { optional += slugs }
}

/**
 * Configures uploading to Modrinth and CurseForge, enabled by the `modrinthToken` and `curseforgeToken` Gradle properties
 * (an empty one, like an unset CI secret, counts as missing).
 * The GitHub release, with every jar in one place, is made by the release workflow.
 */
fun Project.configurePublishing(
    loader: String,
    jar: Provider<RegularFile>,
    mcVersion: String,
    minecraftVersions: List<String>,
    deps: PublishingDependencies.() -> Unit = {},
) {
    val dependencies = PublishingDependencies().apply(deps)
    val modVersion = property("mod.version").toString()

    extensions.configure<ModPublishExtension> {
        file.set(jar)
        displayName.set("$modVersion for $loader $mcVersion")
        version.set(project.version.toString())
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        // Pre-release versions (1.10.0-alpha.1) are uploaded as alpha or beta files
        type.set(when {
            "-alpha" in modVersion -> ReleaseType.ALPHA
            "-beta" in modVersion -> ReleaseType.BETA
            else -> ReleaseType.STABLE
        })
        modLoaders.add(loader)
        // -PpublishDryRun checks everything without uploading
        dryRun.set(providers.gradleProperty("publishDryRun").isPresent)

        providers.gradleProperty("modrinthToken").orNull?.takeIf { it.isNotBlank() }?.let { token ->
            modrinth {
                projectId.set(property("mod.modrinth").toString())
                accessToken.set(token)
                this.minecraftVersions.addAll(minecraftVersions)
                dependencies.required.forEach { requires(it) }
                dependencies.optional.forEach { optional(it) }
            }
        }

        providers.gradleProperty("curseforgeToken").orNull?.takeIf { it.isNotBlank() }?.let { token ->
            curseforge {
                projectId.set(property("mod.curseforge").toString())
                accessToken.set(token)
                // Client-side only mod
                client.set(true)
                server.set(false)
                this.minecraftVersions.addAll(minecraftVersions)
                dependencies.required.forEach { requires(it) }
                dependencies.optional.forEach { optional(it) }
            }
        }
    }
}

/**
 * JUnit on the `test` source set, the same on every loader.
 */
fun Project.configureTests() {
    dependencies.add("testImplementation", dependencies.platform("org.junit:junit-bom:6.1.3"))
    dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter")
    dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    // MixinTargetsTest reads classes with ASM, which every loader already brings along with Mixin

    // Tests go through Stonecutter too, so they can use `//? if` comments
    tasks.named("compileTestJava") { dependsOn("stonecutterGenerateTest") }

    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

/**
 * Where the in-game self-test runs, when it was asked for with `-PsharedResources.selfTest`.
 */
class SelfTestSetup(val dir: java.io.File) {
    val report = java.io.File(dir, "selftest-report.json")
    val global = java.io.File(dir, "global")
}

fun Project.selfTestSetup(): SelfTestSetup? =
    if (providers.gradleProperty("sharedResources.selfTest").isPresent) SelfTestSetup(rootProject.file("run/selftest/$name"))
    else null

interface SelfTestMutex : org.gradle.api.services.BuildService<org.gradle.api.services.BuildServiceParameters.None>

/**
 * Turns [runTask] into the in-game self-test: prepares a clean game directory with every resource shared,
 * and fails the build unless the game reports that every feature worked.
 * The loader build script points its client run at [SelfTestSetup.dir] and passes the report path.
 */
fun Project.configureSelfTest(runTask: String) {
    val setup = selfTestSetup() ?: return
    val resources = listOf("resourcepacks", "saves", "config", "screenshots", "datapacks", "options", "servers", "hotbars")

    val prepare = tasks.register("prepareSelfTest") {
        group = "verification"
        description = "Prepares a game directory for the in-game self-test"
        doLast {
            setup.dir.deleteRecursively()
            val config = java.io.File(setup.dir, "config/shared-resources.json")
            config.parentFile.mkdirs()
            val enabled = resources.joinToString(",\n") { "    \"shared-resources:$it\": true" }
            val root = setup.global.absolutePath.replace("\\", "\\\\")
            config.writeText("{\n  \"globalDirectory\": { \"root\": \"$root\" },\n  \"enabled\": {\n$enabled\n  }\n}\n")

            // An option no version knows, which must survive the game rewriting the shared options.txt
            setup.global.mkdirs()
            java.io.File(setup.global, "options.txt").writeText("onboardAccessibility:false\nsharedResourcesSelfTest:kept\n")
        }
    }

    val check = tasks.register("checkSelfTest") {
        group = "verification"
        description = "Fails unless the in-game self-test passed"
        doLast {
            if (!setup.report.isFile) {
                throw org.gradle.api.GradleException("No self-test report at ${setup.report}, the game crashed or never loaded. See ${setup.dir}/logs")
            }
            @Suppress("UNCHECKED_CAST")
            val report = groovy.json.JsonSlurper().parse(setup.report) as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val checks = report["checks"] as List<Map<String, Any>>
            checks.forEach { logger.lifecycle("  ${if (it["passed"] == true) "PASS" else "FAIL"} ${it["name"]}: ${it["detail"]}") }
            val failed = checks.filter { it["passed"] != true }
            if (checks.isEmpty() || failed.isNotEmpty()) {
                throw org.gradle.api.GradleException("Self-test failed for $name: ${failed.joinToString { it["name"].toString() }}")
            }
        }
    }

    // Only one game at a time, a machine can only take so many
    val mutex = gradle.sharedServices.registerIfAbsent("selfTestMutex", SelfTestMutex::class.java) {
        maxParallelUsages.set(1)
    }
    // Some toolchains register their run tasks late, so match by name instead of looking it up now
    tasks.matching { it.name == runTask }.configureEach {
        dependsOn(prepare)
        finalizedBy(check)
        usesService(mutex)
    }
}
