package com.crazylimits.shared_resources.mixin.resourcepacks;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FolderRepositorySource.class)
public interface FileResourcePackProviderAccessor {
    @Accessor("packType")
    PackType sharedresources$getResourceType();
}
