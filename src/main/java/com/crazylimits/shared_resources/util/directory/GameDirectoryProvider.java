package com.crazylimits.shared_resources.util.directory;

import com.crazylimits.shared_resources.api.GameResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface GameDirectoryProvider {
    @Nullable
    Path getDirectory(GameResource resource);
}
