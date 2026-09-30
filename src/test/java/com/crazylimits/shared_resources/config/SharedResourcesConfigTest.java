package com.crazylimits.shared_resources.config;

import com.crazylimits.shared_resources.SharedResources;
import com.crazylimits.shared_resources.api.ResourceDirectory;
import com.crazylimits.shared_resources.api.ResourceDirectoryBuilder;
import com.crazylimits.shared_resources.util.directory.EmptyGameDirectoryProvider;
import com.crazylimits.shared_resources.util.directory.RootedGameDirectoryProvider;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedResourcesConfigTest {
    @TempDir
    Path dir;

    private final Identifier saves = SharedResources.id("saves");
    private final Identifier screenshots = SharedResources.id("screenshots");

    @Test
    void roundTripsThroughJson() {
        File file = dir.resolve("config/shared-resources.json").toFile();
        SharedResourcesConfig config = new SharedResourcesConfig();
        config.setGlobalDirectory(Paths.get("/games/global"));
        config.setEnabled(saves, true);
        config.setEnabled(screenshots, false);

        config.saveConfigFile(file);
        SharedResourcesConfig loaded = SharedResourcesConfig.loadConfigFile(file);

        RootedGameDirectoryProvider global = assertInstanceOf(RootedGameDirectoryProvider.class, loaded.getGlobalDirectory());
        assertEquals(Paths.get("/games/global"), global.getRoot());
        assertTrue(loaded.isEnabled(saves));
        assertFalse(loaded.isEnabled(screenshots));
        assertEquals(config.getEnabled(), loaded.getEnabled());
    }

    @Test
    void noGlobalDirectorySurvivesSaving() {
        File file = dir.resolve("shared-resources.json").toFile();
        SharedResourcesConfig config = new SharedResourcesConfig();
        config.setGlobalDirectory(null);
        config.setEnabled(saves, true);

        config.saveConfigFile(file);
        SharedResourcesConfig loaded = SharedResourcesConfig.loadConfigFile(file);

        assertInstanceOf(EmptyGameDirectoryProvider.class, loaded.getGlobalDirectory());
        assertFalse(loaded.isEnabled(saves), "Nothing is shared without a global directory");
    }

    @Test
    void missingOrEmptyFileGivesDefaults() throws Exception {
        SharedResourcesConfig missing = SharedResourcesConfig.loadConfigFile(dir.resolve("missing.json").toFile());
        Path empty = Files.writeString(dir.resolve("empty.json"), "", StandardCharsets.UTF_8);
        SharedResourcesConfig blank = SharedResourcesConfig.loadConfigFile(empty.toFile());

        for (SharedResourcesConfig config : new SharedResourcesConfig[]{missing, blank}) {
            assertInstanceOf(RootedGameDirectoryProvider.class, config.getGlobalDirectory());
            assertTrue(config.getEnabled().isEmpty());
        }
    }

    @Test
    void readsTheSavedFormat() throws Exception {
        Path file = Files.writeString(dir.resolve("shared-resources.json"), """
                {
                  "globalDirectory": { "root": "global_resources" },
                  "enabled": { "shared-resources:saves": true, "shared-resources:options": false }
                }
                """, StandardCharsets.UTF_8);

        SharedResourcesConfig config = SharedResourcesConfig.loadConfigFile(file.toFile());

        assertEquals(Paths.get("global_resources"), ((RootedGameDirectoryProvider) config.getGlobalDirectory()).getRoot());
        assertTrue(config.isEnabled(saves));
        assertFalse(config.isEnabled(SharedResources.id("options")));
    }

    @Test
    void resourcesResolveUnderTheGlobalDirectory() {
        SharedResourcesConfig config = new SharedResourcesConfig();
        ResourceDirectory directory = new ResourceDirectoryBuilder("screenshots").build();

        config.setGlobalDirectory(Paths.get("/games/global"));
        assertEquals(Paths.get("/games/global/screenshots"), config.getDirectory(directory));

        config.setGlobalDirectory(null);
        assertNull(config.getDirectory(directory));
    }
}
