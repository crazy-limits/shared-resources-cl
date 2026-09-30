# Shared Resources

![Mod Loaders](https://img.shields.io/badge/Mod%20Loaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-lightyellow)
![Enviroment](https://img.shields.io/badge/Enviroment-Client-purple)
[![Discord](https://img.shields.io/discord/1016206797389975612?color=blue&logo=discord&label=Discord)](https://discord.gg/WcYsDDQtyR)

[![Modrinth](https://img.shields.io/modrinth/dt/shared-resources?color=00AF5C&label=downloads&logo=modrinth)](https://modrinth.com/mod/shared-resources)
[![CurseForge](https://cf.way2muchnoise.eu/full_679045_downloads.svg)](https://curseforge.com/minecraft/mc-mods/shared-resources)

> **This is a fork** of [Shared Resources](https://github.com/enjarai/shared-resources) by
> [enjarai](https://github.com/enjarai), maintained at
> [crazy-limits/shared-resources-cl](https://github.com/crazy-limits/shared-resources-cl).
> It ports the mod to NeoForge and Forge and to Minecraft 26.1–26.3. All credit for the mod itself
> goes to the original authors. The official releases are on
> [Modrinth](https://modrinth.com/mod/shared-resources) and
> [CurseForge](https://www.curseforge.com/minecraft/mc-mods/shared-resources).
> The download badges above are for the original mod, and the Discord is the original author's.

A mod for easily sharing game files between separate Minecraft instances. 
Best used with a multi-instance launcher like [MultiMC](https://multimc.org/) or [Prism Launcher](https://prismlauncher.org/).

It works by selecting a global directory to hold your common game files, 
and modifying Minecraft to use that directory instead of the default one.
Where possible, the mod tries to load files from the global directory together 
with the local ones, merging the two and allowing you to select from both. 
This is currently only available for resource packs, data packs and shaders.

Currently supported game files/directories:
- Resource packs
- Data packs
- Saves
- Vanilla options (video settings, controls, etc.)
- Servers
- Screenshots
- Saved Hotbars
- The config folder (Fabric only, should work with the configs of any properly coded mods)
- Shader packs (via Iris)
- Schematics (via Litematica)
- Replay Recordings (via ReplayMod)
- Skin Presets (via SkinShuffle)
- World Map Data (via Xaero's World Map, Fabric only)

Available for Fabric, NeoForge and Forge on Minecraft 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1, 26.2 and 26.3
(NeoForge from 1.21.1). The in-game config screen needs [Cloth Config](https://modrinth.com/mod/cloth-config),
without it the config is edited in `config/shared-resources.json`. Cloth Config isn't released for Forge after 1.21.4.

Incorporates code from [RememberMyTxt](https://github.com/DuncanRuns/RememberMyTxt)
(with permission from [DuncanRuns](https://github.com/DuncanRuns))
to ensure game options aren't lost when sharing the options.txt file
between different game versions and mod sets.

## API

A robust API is available for other mods to use, allowing them to easily
add support for their own game files and directories.

The API ships inside the main mod, in the `com.crazylimits.shared_resources.api` package.
Depend on the mod at compile time only, and only touch API classes when it is loaded
(`shared-resources` on Fabric, `shared_resources` on NeoForge and Forge).

On Fabric, create a Shared Resources entrypoint in your mod's `fabric.mod.json`:

```json
{
  "entrypoints": {
    "shared-resources": [
      "com.example.modid.GameResources"
    ]
  }
}
```

On NeoForge and Forge, register it as a Java service instead, by listing the class in
`META-INF/services/com.crazylimits.shared_resources.api.SharedResourcesEntrypoint`.

Make sure to implement the `SharedResourcesEntrypoint` interface in your entrypoint class, 
and use that to create and register your `GameResource` instances, each corresponding to a game file or directory.
**This entrypoint runs very early (Fabric's preLaunch phase), limit your interaction with Minecraft classes to a minimum.**

```java
public class GameResources implements SharedResourcesEntrypoint {
    // You can manually implement these interfaces, but a builder is provided for convenience
    public static final ResourceDirectory MY_CUSTOM_DIRECTORY = new ResourceDirectoryBuilder("custom_directory")
            .setDisplayName(Component.translatable("modid.directory.custom_directory"))
            .setDescription(
                    Component.translatable("modid.directory.custom_directory.description[0]"),
                    Component.translatable("modid.directory.custom_directory.description[1]")
            )
            .requiresRestart() // Set this if the directory requires a restart to take effect
            .overridesDefaultDirectory() // Set this if the directory completely overrides the default one
            .defaultEnabled(false) // You can probably guess what this does
            .isExperimental() // Set this to warn the user that issues may arise
            .build(); // For a comprehensive list of the available options, see the javadocs
    public static final ResourceFile A_CUSTOM_FILE = new ResourceFileBuilder("custom_file")
            .setDisplayName(Component.translatable("modid.file.custom_file"))
            .build();
    
    @Override
    public void registerResources(GameResourceRegistry registry) {
        // Don't forget to register your resources
        registry.register(ResourceLocation.fromNamespaceAndPath("modid", "custom_directory"), MY_CUSTOM_DIRECTORY);
        registry.register(ResourceLocation.fromNamespaceAndPath("modid", "custom_file"), A_CUSTOM_FILE);
    }
}
```

After registering your resources, they will automatically show up in the main mod's config screen, if it is loaded.

Finally, you can use `GameResourceHelper` to get the path to the location of your resource when loading it:

```java
// If a global directory is selected, this will return the path to the global directory
Path dirLocation = GameResourceHelper.getPathFor(GameResources.MY_CUSTOM_DIRECTORY);

// Make sure to check if its null before using it
if (dirLocation != null) {
    loadYourStuffFunction(dirLocation);
}
    

// Try to load from your default directory as well when possible
loadYourStuffFunction(GameResources.MY_CUSTOM_DIRECTORY.getDefaultPath());
```

## Building

Every Minecraft version and loader is a [Stonecutter](https://stonecutter.kikugie.dev/) subproject named
`{version}-{loader}`, e.g. `:1.21.11-neoforge`. Sources live once in `src/main` and use Mojang's official names,
with `//? if` comments for version and loader differences. Versions and dependencies are declared in
`settings.gradle.kts` and `stonecutter.properties.toml`.

Gradle runs on Java 25. JDKs 17 and 21 are needed for 1.20.1 and 1.21.x, and are downloaded if missing.

```bash
./gradlew buildAll                    # build everything
./gradlew collectAll                  # build everything and copy the jars to build/libs/{mod version}/
./gradlew :1.21.11-fabric:build       # build one target
./gradlew :26.3-neoforge:runClient    # run one target, each gets its own run/{target} directory
```

### Tests

```bash
./gradlew testAll                                       # unit and mixin tests on every target
./gradlew selfTestAll -PsharedResources.selfTest        # launch every target and check each feature in game
./gradlew :1.21.1-neoforge:runClient -PsharedResources.selfTest   # the same for one target
```

- **Unit tests** (`src/test`) cover the API, path resolution and the config file.
- **`MixinTargetsTest`** reads the compiled mixins and checks every target class, shadow, accessor,
  target method and injection point against that target's Minecraft, catching renamed methods
  and changed call sites at build time.
- **The self-test** launches the game with every resource shared into `run/selftest/{target}/global`,
  checks options (including unknown options surviving), saves, resource packs, data packs, servers,
  hotbars, screenshots and (on Fabric) the config folder, then quits. The build fails if any check does.
  Mod compat (Iris, Litematica, ...) isn't covered, those mods aren't installed in the test game.

`.sc_active_version` picks the version the IDE and the sources on disk are switched to.
Change it and run `./gradlew stonecutterGenerate`, or use the "Set active project" tasks in the `stonecutter` group.

Fabric uses Loom (through loom-back-compat for 26.1+), NeoForge uses ModDevGradle,
Forge uses ForgeGradle 7, except Forge 1.20.1 which uses ModDevGradle Legacy.

## Releasing

Commits follow [Conventional Commits](https://www.conventionalcommits.org), which pull requests are checked for:
`type(scope): subject`, with type one of `feat`, `fix`, `perf`, `refactor`, `revert`, `docs`, `style`, `test`,
`build`, `ci` or `chore`. The next version is picked from them with [semantic versioning](https://semver.org):

| Commits since the last release           | Bump                 |
|------------------------------------------|----------------------|
| `feat!: ...` or a `BREAKING CHANGE:` footer | major (`2.0.0`)   |
| `feat: ...`                              | minor (`1.11.0`)     |
| `fix: ...`, `perf: ...`                  | patch (`1.10.1`)     |
| anything else                            | no release           |

To release, push a trigger tag on the latest commit of `master`:

```bash
git tag -f release && git push -f origin release                # stable: 1.10.0
git tag -f release-beta && git push -f origin release-beta      # beta: 1.10.0-beta.1, beta.2, ...
git tag -f release-alpha && git push -f origin release-alpha    # alpha: 1.10.0-alpha.1, alpha.2, ...
```

Alpha and beta versions are [semver pre-releases](https://semver.org/#spec-item-9) of the next version, numbered
after the ones already released. They're published as GitHub pre-releases and as alpha or beta files on
Modrinth and CurseForge. Release notes list the changes since the previous release on any channel, or, for a
stable release, since the last stable one.

The [release workflow](.github/workflows/release.yml) then sets `mod.version`, writes the release notes to
`CHANGELOG.md`, builds and tests every target, pushes a `chore(release): x.y.z` commit tagged `x.y.z`, creates
the GitHub release with all jars and uploads to Modrinth and CurseForge if the `MODRINTH_TOKEN` and
`CURSEFORGE_TOKEN` secrets are set. It can also be started from the Actions tab to pick the channel or force a bump level.
If `mod.version` was already raised by hand past the last release, that version is used instead of a lower one.

Preview the next version locally with `python3 .github/scripts/release.py next`.

## Credits

- [enjarai](https://github.com/enjarai), who created Shared Resources, and [jacg](https://github.com/jacg), co-author.
  Original source: [enjarai/shared-resources](https://github.com/enjarai/shared-resources)
- Downloads of the original mod: [Modrinth](https://modrinth.com/mod/shared-resources) ·
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/shared-resources)
- [DuncanRuns](https://github.com/DuncanRuns), for [RememberMyTxt](https://github.com/DuncanRuns/RememberMyTxt)
- Everyone who contributed to the original project

This fork is distributed under the same license as the original.

## Modifications

As required by the LGPLv3, this is a modified version of Shared Resources.
The original copyright belongs to its authors, the changes are by crazy-limits (2026):

- Ported to NeoForge and Forge next to Fabric, and to Minecraft 1.21.11 and 26.1–26.3
- Moved from Yarn to Mojang's official mappings, with a Stonecutter multi-version build
- Moved the code from `nl.enjarai.shared_resources` to `com.crazylimits.shared_resources`
- Folded the API into the main mod, made Cloth Config optional and dropped Cicada
- Fixed mixins that no longer applied on newer versions, added tests

## License

This mod is under the [GNU Lesser General Public License v3.0](LICENSE).
