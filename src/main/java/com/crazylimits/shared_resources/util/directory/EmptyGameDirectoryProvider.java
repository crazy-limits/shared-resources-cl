package com.crazylimits.shared_resources.util.directory;

import com.crazylimits.shared_resources.api.GameResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class EmptyGameDirectoryProvider implements GameDirectoryProvider {
    @Nullable
    @Override
    public Path getDirectory(GameResource resource) {
        return null;
    }
}
