pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Switches between remapping Loom (obfuscated versions) and plain Loom (26.1+)
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Creates `versions/{version}-{loader}` nodes, each built by `build.{loader}.gradle.kts`.
         */
        fun match(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.20.1", "fabric")
        // ForgeGradle 7 can't set up Forge 1.20.1, which still runs on SRG names
        version("1.20.1-forge", "1.20.1").buildscript("build.legacyforge.gradle.kts")
        match("1.21.1", "fabric", "neoforge", "forge")
        match("1.21.4", "fabric", "neoforge", "forge")
        match("1.21.11", "fabric", "neoforge", "forge")
        match("26.1.2", "fabric", "neoforge", "forge")
        match("26.2", "fabric", "neoforge", "forge")
        match("26.3", "fabric", "neoforge", "forge")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "shared-resources"
