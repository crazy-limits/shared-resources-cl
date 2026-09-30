plugins {
    id("dev.kikugie.stonecutter")
    id("dev.kikugie.loom-back-compat") apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
    id("net.minecraftforge.gradle") version "7.0.40" apply false
    id("net.minecraftforge.jarjar") version "0.2.3" apply false
    id("me.modmuss50.mod-publish-plugin") apply false
}

// The version the IDE and plain `./gradlew` commands operate on
stonecutter active file(".sc_active_version")

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Applies version- and loader-specific properties from `stonecutter.properties.toml`
    properties {
        tags(version, loader)
    }

    // `//? if fabric {`, `//? if neoforge {`, `//? if forge {` and `//? if forgelike {`
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }
    constants["forgelike"] = loader != "fabric"
    // Cloth Config (and so the config screen) isn't available on every loader/version
    constants["cloth"] = properties.getOrNull<String>("deps.cloth_config").orEmpty().isNotEmpty()

    replacements {
        // Mojang renamed ResourceLocation to Identifier in 1.21.11
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "26.1") {
            replace("classTweaker v2 named", "classTweaker v2 official")
        }
    }
}

tasks.register("buildAll") {
    group = "stonecutter"
    description = "Builds every version/loader combination"
    dependsOn(stonecutter.versions.map { ":${it.project}:build" })
}

tasks.register("testAll") {
    group = "stonecutter"
    description = "Runs the unit and mixin target tests on every version/loader combination"
    dependsOn(stonecutter.versions.map { ":${it.project}:test" })
}

tasks.register("selfTestAll") {
    group = "stonecutter"
    description = "Launches every version/loader combination and checks every feature in game, one at a time"
    if (providers.gradleProperty("sharedResources.selfTest").isPresent) {
        dependsOn(stonecutter.versions.map { ":${it.project}:runClient" })
    } else doFirst {
        throw GradleException("Run with -PsharedResources.selfTest, e.g. ./gradlew selfTestAll -PsharedResources.selfTest")
    }
}

tasks.register("collectAll") {
    group = "stonecutter"
    description = "Builds every version/loader combination and copies the jars to `build/libs/{mod version}/`"
    dependsOn(stonecutter.versions.map { ":${it.project}:buildAndCollect" })
}

tasks.register("publishAll") {
    group = "stonecutter"
    description = "Publishes every version/loader combination"
    dependsOn(stonecutter.versions.map { ":${it.project}:publishMods" })
}

tasks.register("runActiveClient") {
    group = "stonecutter"
    description = "Runs the client for whichever version .sc_active_version points at"
    dependsOn("${stonecutter.current!!.project}:runClient")
}
