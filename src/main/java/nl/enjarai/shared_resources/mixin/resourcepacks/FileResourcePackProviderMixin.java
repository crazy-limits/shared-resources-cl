package nl.enjarai.shared_resources.mixin.resourcepacks;

import nl.enjarai.shared_resources.util.FileResourcepackProviderProxy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import net.minecraft.server.packs.repository.FolderRepositorySource;

@Mixin(FolderRepositorySource.class)
public class FileResourcePackProviderMixin implements FileResourcepackProviderProxy {
    @Mutable
    @Shadow @Final private Path folder;

    @Override
    public void sharedresources$setPacksFolder(Path folder) {
        this.folder = folder;
    }

    @Override
    public Path sharedresources$getPacksFolder() {
        return this.folder;
    }
}
