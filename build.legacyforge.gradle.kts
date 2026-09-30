plugins {
    id("net.neoforged.moddev.legacyforge")
    id("me.modmuss50.mod-publish-plugin")
    id("neoforge-mutex")
}

version = "${project.property("mod.version")}+${sc.current.version}-forge"
base.archivesName = project.property("mod.id") as String

val requiredJava = requiredJavaFor(sc.current.version)
val selfTest = selfTestSetup()
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
val clothVersion = sc.properties.getOrNull<String>("deps.cloth_config").orEmpty()
// Forge 1.20.1 runs on SRG names, so the jar is remapped and mixins need a refmap
val obfuscatedRuntime = true

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.shedaniel.me/", "Shedaniel", "me.shedaniel.cloth", "me.shedaniel")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.5.0")!!)

    // Optional, only provides the config screen. Cloth Config has no Forge builds after 1.21.4.
    if (clothVersion.isNotEmpty()) {
        modCompileOnly("me.shedaniel.cloth:cloth-config-forge:$clothVersion") { isTransitive = false }
        modRuntimeOnly("me.shedaniel.cloth:cloth-config-forge:$clothVersion") { isTransitive = false }
    }
}

legacyForge {
    version = project.property("deps.forge") as String
    accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

    // Minecraft on the test classpath, for MixinTargetsTest and friends
    addModdingDependenciesTo(sourceSets.test.get())

    mods {
        register("shared_resources") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            client()
            // One run directory per target, so versions never share worlds or configs
            gameDirectory = selfTest?.dir ?: rootProject.file("run/${sc.current.project}")
            jvmArgument("-Dmixin.debug.export=true")
            selfTest?.let { jvmArgument("-Dshared-resources.selftest=${it.report.absolutePath}") }
        }
    }
}

mixin {
    add(sourceSets.main.get(), "shared-resources.refmap.json")
    config("shared-resources.mixins.json")
    config("shared-resources.compat.mixins.json")
}

configureTests()
configureSelfTest("runClient")

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

// The jar that actually runs outside of dev
val modJar: TaskProvider<out Jar> = tasks.named<Jar>("reobfJar")

tasks {
    processResources {
        val props = mapOf(
            "name" to project.property("mod.name") as String,
            "version" to version.toString(),
            "minecraft" to project.property("mod.mc_compat") as String,
        )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("java", mixinJava)
        inputs.property("refmap", obfuscatedRuntime)
        filesMatching("*.mixins.json") {
            expand("java" to mixinJava)
            // Mixin needs the refmap to find SRG names in production
            if (obfuscatedRuntime) filter { line ->
                if ("\"package\"" in line) "$line\n  \"refmap\": \"shared-resources.refmap.json\"," else line
            }
        }

        exclude("fabric.mod.json", "*.ct", "META-INF/neoforge.mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    jar {
        manifest.attributes("MixinConfigs" to "shared-resources.mixins.json,shared-resources.compat.mixins.json")
    }

    withType<Jar> {
        from(rootProject.file("LICENSE")) { rename { "${it}_${project.property("mod.id")}" } }
    }

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
