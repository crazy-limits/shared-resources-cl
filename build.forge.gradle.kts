plugins {
    id("net.minecraftforge.gradle")
    id("net.minecraftforge.jarjar")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${project.property("mod.version")}+${sc.current.version}-forge"
base.archivesName = project.property("mod.id") as String

val requiredJava = requiredJavaFor(sc.current.version)
val selfTest = selfTestSetup()
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
val clothVersion = sc.properties.getOrNull<String>("deps.cloth_config").orEmpty()

minecraft {
    // 26.1+ ships without obfuscation, older versions get Mojang's mappings
    if (sc.current.parsed < "26.1") mappings("official", sc.current.version)
    accessTransformers = files(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        configureEach {
            // One run directory per target, so versions never share worlds or configs
            workingDir.convention(rootProject.layout.projectDirectory.dir(selfTest?.dir?.path ?: "run/${sc.current.project}"))
            args("--mixin.config=shared-resources.mixins.json", "--mixin.config=shared-resources.compat.mixins.json")
            systemProperty("mixin.debug.export", "true")
            selfTest?.let { systemProperty("shared-resources.selftest", it.report.absolutePath) }
            // GLFW and SDL need the main thread on macOS, ForgeGradle doesn't add this itself
            if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
        }
        register("client")
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.shedaniel.me/") { name = "Shedaniel" } }
        filter { includeGroup("me.shedaniel.cloth"); includeGroup("me.shedaniel") }
    }
}

// Ships MixinExtras inside the mod jar
jarJar.register {
    archiveClassifier = null
}
tasks.jar {
    archiveClassifier = "slim"
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${project.property("deps.forge")}"))
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.0")
    "jarJar"("io.github.llamalad7:mixinextras-forge:0.5.0")
    // Dev runs load the mod from its classes, so the jar-in-jar copy isn't there
    runtimeOnly("io.github.llamalad7:mixinextras-forge:0.5.0")

    // Optional, only provides the config screen. Cloth Config has no Forge builds after 1.21.4.
    if (clothVersion.isNotEmpty()) {
        compileOnly("me.shedaniel.cloth:cloth-config-forge:$clothVersion") { isTransitive = false }
        runtimeOnly("me.shedaniel.cloth:cloth-config-forge:$clothVersion") { isTransitive = false }
    }
}

// Compile Stonecutter's processed sources, the same as on Fabric
sourceSets.main {
    java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/java")))
}
sourceSets.test {
    java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/test/java")))
}

configureTests()
configureSelfTest("runClient")

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

val modJar = tasks.named<Jar>("jarJar")

tasks {
    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
        options.encoding = "UTF-8"
    }
    withType<Jar>().configureEach { dependsOn("stonecutterGenerate") }

    processResources {
        dependsOn("stonecutterGenerate")
        val props = mapOf(
            "name" to project.property("mod.name") as String,
            "version" to version.toString(),
            "minecraft" to project.property("mod.mc_compat") as String,
        )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }

        // Forge's Mixin stops at JAVA_21, it still loads newer class files
        val mixinJava = "JAVA_${minOf(requiredJava.majorVersion.toInt(), 21)}"
        inputs.property("java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "*.ct", "META-INF/neoforge.mods.toml")
    }

    withType<Jar>().configureEach {
        // Mixin finds our configs through the manifest
        manifest.attributes("MixinConfigs" to "shared-resources.mixins.json,shared-resources.compat.mixins.json")
    }

    // jarJar repackages this jar, so the license only goes in here
    jar {
        from(rootProject.file("LICENSE")) { rename { "${it}_${project.property("mod.id")}" } }
    }

    assemble { dependsOn(modJar) }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to `build/libs/{mod version}/`"
        from(modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

configurePublishing("forge", modJar.flatMap { it.archiveFile }, sc.current.version, mcReleases) {
    if (clothVersion.isNotEmpty()) optional("cloth-config")
}
