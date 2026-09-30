package com.crazylimits.shared_resources.api;

import com.crazylimits.shared_resources.util.GameResourceConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameResourceHelperTest {
    @TempDir
    Path global;

    private final Set<GameResource> enabled = new HashSet<>();
    private final ResourceDirectory directory = new ResourceDirectoryBuilder("resourcepacks").build();
    private final ResourceFile file = new ResourceFileBuilder("config/nested/options.txt").build();

    @BeforeEach
    void setUp() {
        GameResourceHelper.setConfigSource(new GameResourceConfig() {
            @Override
            public boolean isEnabled(GameResource resource) {
                return enabled.contains(resource);
            }

            @Override
            public @Nullable Path getDirectory(GameResource resource) {
                return global.resolve(resource.getDefaultPath());
            }
        });
    }

    @AfterEach
    void tearDown() {
        GameResourceHelper.setConfigSource(null);
    }

    @Test
    void disabledResourceHasNoPath() {
        assertNull(GameResourceHelper.getPathFor(directory));
    }

    @Test
    void nullResourceHasNoPath() {
        assertNull(GameResourceHelper.getPathFor(null));
    }

    @Test
    void enabledDirectoryResolvesUnderGlobalDirectoryAndIsCreated() {
        enabled.add(directory);

        Path path = GameResourceHelper.getPathFor(directory);

        assertEquals(global.resolve("resourcepacks"), path);
        assertTrue(Files.isDirectory(path), "Directory should be created");
    }

    @Test
    void enabledFileGetsItsParentCreatedButNotItself() {
        enabled.add(file);

        Path path = GameResourceHelper.getPathFor(file);

        assertEquals(global.resolve("config/nested/options.txt"), path);
        assertTrue(Files.isDirectory(path.getParent()), "Parent directory should be created");
        assertTrue(Files.notExists(path), "The file itself should be left to the game");
    }

    @Test
    void fallsBackToGivenDefaultWhenDisabled(@TempDir Path local) {
        Path fallback = local.resolve("resourcepacks");

        assertEquals(fallback, GameResourceHelper.getPathOrDefaultFor(directory, fallback));

        enabled.add(directory);
        assertEquals(global.resolve("resourcepacks"), GameResourceHelper.getPathOrDefaultFor(directory, fallback));
    }
}
