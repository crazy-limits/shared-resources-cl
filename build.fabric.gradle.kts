plugins {
    // Applies the remapping Loom on obfuscated versions and plain Loom on 26.1+
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${project.property("mod.version")}+${sc.current.version}-fabric"
base.archivesName = project.property("mod.id") as String

val requiredJava = requiredJavaFor(sc.current.version)
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.terraformersmc.com/releases", "TerraformersMC", "com.terraformersmc")
    strictMaven("https://maven.shedaniel.me/", "Shedaniel", "me.shedaniel.cloth", "me.shedaniel")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang mappings on obfuscated versions, no-op on 26.1+
    loomx.applyMojangMappings()

    // Use `mod{configuration}` everywhere, loom-back-compat converts them on 26.1+
    modImplementation("net.fabricmc:fabric-loader:${project.property("deps.fabric_loader")}")

    // Optional integrations, only needed at runtime in dev
    modCompileOnly("com.terraformersmc:modmenu:${project.property("deps.modmenu")}") { isTransitive = false }
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${project.property("deps.cloth_config")}") { isTransitive = false }
    modLocalRuntime("com.terraformersmc:modmenu:${project.property("deps.modmenu")}") { isTransitive = false }
    modLocalRuntime("me.shedaniel.cloth:cloth-config-fabric:${project.property("deps.cloth_config")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${project.property("deps.fabric_api")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/shared-resources.ct"),
        "build/processed.ct"
    )

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Names lambdas, useful for mixins
    }

    runConfigs.all {
        generateRunConfig = true
        // One run directory per target, so versions never share worlds or configs
        runDirectory = rootProject.file("run/${sc.current.project}")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

// Loom rebuilds the source set after Stonecutter substitutes it, so point at the processed copy by hand.
// Without this the raw `src/main` tree is compiled and every `//? if` branch is silently ignored.
sourceSets.main {
    java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/java")))
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    withType<JavaCompile>().configureEach { dependsOn("stonecutterGenerate") }
    withType<Jar>().configureEach { dependsOn("stonecutterGenerate") }

    processResources {
        dependsOn("stonecutterGenerate")
        val props = mapOf(
            "id" to project.property("mod.id") as String,
            "name" to project.property("mod.name") as String,
            "version" to version.toString(),
            "minecraft" to project.property("mod.mc_compat") as String,
            "loader" to Regex("\\d+\\.\\d+").find(project.property("deps.fabric_loader") as String)!!.value,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg")
    }

    withType<Jar> {
        from(rootProject.file("LICENSE")) { rename { "${it}_${project.property("mod.id")}" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to `build/libs/{mod version}/`"
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

configurePublishing("fabric", loomx.modJar.flatMap { it.archiveFile }, sc.current.version, mcReleases) {
    optional("cloth-config", "modmenu")
}
