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
 * Configures uploading to Modrinth, CurseForge and GitHub, enabled by the matching `enjarai*Token` Gradle properties.
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
        type.set(ReleaseType.STABLE)
        modLoaders.add(loader)

        providers.gradleProperty("enjaraiModrinthToken").orNull?.let { token ->
            modrinth {
                projectId.set(property("mod.modrinth").toString())
                accessToken.set(token)
                this.minecraftVersions.addAll(minecraftVersions)
                dependencies.required.forEach { requires(it) }
                dependencies.optional.forEach { optional(it) }
            }
        }

        providers.gradleProperty("enjaraiCurseforgeToken").orNull?.let { token ->
            curseforge {
                projectId.set(property("mod.curseforge").toString())
                accessToken.set(token)
                this.minecraftVersions.addAll(minecraftVersions)
                dependencies.required.forEach { requires(it) }
                dependencies.optional.forEach { optional(it) }
            }
        }

        providers.gradleProperty("enjaraiGithubToken").orNull?.let { token ->
            github {
                repository.set(property("mod.github").toString())
                accessToken.set(token)
                commitish.set("master")
                tagName.set(project.version.toString())
            }
        }
    }
}
