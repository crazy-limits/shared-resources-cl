package com.crazylimits.shared_resources.util.directory;

import com.crazylimits.shared_resources.api.GameResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class RootedGameDirectoryProvider implements GameDirectoryProvider {
    protected Path root;

    public RootedGameDirectoryProvider(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    @Nullable
    @Override
    public Path getDirectory(GameResource resource) {
        return getRoot().resolve(resource.getDefaultPath());
    }
}
