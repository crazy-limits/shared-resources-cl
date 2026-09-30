plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
    id("neoforge-mutex")
}

version = "${project.property("mod.version")}+${sc.current.version}-neoforge"
base.archivesName = project.property("mod.id") as String

val requiredJava = requiredJavaFor(sc.current.version)
val selfTest = selfTestSetup()
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.shedaniel.me/", "Shedaniel", "me.shedaniel.cloth", "me.shedaniel")
}

dependencies {
    // Optional, only provides the config screen
    compileOnly("me.shedaniel.cloth:cloth-config-neoforge:${project.property("deps.cloth_config")}") { isTransitive = false }
    runtimeOnly("me.shedaniel.cloth:cloth-config-neoforge:${project.property("deps.cloth_config")}") { isTransitive = false }
}

neoForge {
    version = project.property("deps.neoforge") as String
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

configureTests()
configureSelfTest("runClient")

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    processResources {
        val props = mapOf(
            "name" to project.property("mod.name") as String,
            "version" to version.toString(),
            "minecraft" to project.property("mod.mc_compat") as String,
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "*.ct", "META-INF/mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<Jar> {
        from(rootProject.file("LICENSE")) { rename { "${it}_${project.property("mod.id")}" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to `build/libs/{mod version}/`"
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

configurePublishing("neoforge", tasks.jar.flatMap { it.archiveFile }, sc.current.version, mcReleases) {
    optional("cloth-config")
}
