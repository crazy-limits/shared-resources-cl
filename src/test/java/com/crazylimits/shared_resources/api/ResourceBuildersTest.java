package com.crazylimits.shared_resources.api;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBuildersTest {
    @Test
    void directoryDefaults() {
        ResourceDirectory directory = new ResourceDirectoryBuilder("shaderpacks").build();

        assertEquals(Paths.get("shaderpacks"), directory.getDefaultPath());
        assertTrue(directory.isDefaultEnabled());
        assertFalse(directory.isRequiresRestart());
        assertFalse(directory.isOverridesDefaultDirectory());
        assertFalse(directory.isExperimental());
        assertTrue(directory.getDescription().isEmpty());
        assertTrue(directory.getMixinPackages().isEmpty());
    }

    @Test
    void directoryFlags() {
        Component name = Component.literal("Name");
        Component line = Component.literal("Line");
        ResourceDirectory directory = new ResourceDirectoryBuilder(Paths.get("config", "mod"))
                .setDisplayName(name)
                .setDescription(line)
                .requiresRestart()
                .overridesDefaultDirectory()
                .defaultEnabled(false)
                .isExperimental()
                .addMixinPackage("iris")
                .build();

        assertEquals(Paths.get("config", "mod"), directory.getDefaultPath());
        assertSame(name, directory.getDisplayName());
        assertEquals(List.of(line), directory.getDescription());
        assertTrue(directory.isRequiresRestart());
        assertTrue(directory.isOverridesDefaultDirectory());
        assertFalse(directory.isDefaultEnabled());
        assertTrue(directory.isExperimental());
        assertEquals(List.of("iris"), directory.getMixinPackages());
    }

    @Test
    void fileFlags() {
        ResourceFile file = new ResourceFileBuilder("servers.dat")
                .requiresRestart()
                .defaultEnabled(false)
                .addMixinPackage("servers")
                .build();

        assertEquals(Paths.get("servers.dat"), file.getDefaultPath());
        assertTrue(file.isRequiresRestart());
        assertFalse(file.isDefaultEnabled());
        assertEquals(List.of("servers"), file.getMixinPackages());
    }

    @Test
    void updateCallbackIsCalled(@TempDir Path dir) {
        AtomicReference<Path> updated = new AtomicReference<>();
        ResourceDirectory directory = new ResourceDirectoryBuilder("saves").setUpdateCallback(updated::set).build();

        directory.getUpdateCallback().onUpdate(dir);

        assertEquals(dir, updated.get());
    }

    @Test
    void ensureExistsCreatesDirectories(@TempDir Path root) {
        ResourceDirectory directory = new ResourceDirectoryBuilder("a/b").build();
        ResourceFile file = new ResourceFileBuilder("c/d.txt").build();

        directory.ensureExists(root.resolve("a/b"));
        file.ensureExists(root.resolve("c/d.txt"));

        assertTrue(Files.isDirectory(root.resolve("a/b")));
        assertTrue(Files.isDirectory(root.resolve("c")));
        assertFalse(Files.exists(root.resolve("c/d.txt")));
    }
}
