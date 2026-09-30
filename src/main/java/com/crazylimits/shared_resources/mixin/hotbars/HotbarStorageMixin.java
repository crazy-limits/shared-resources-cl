package com.crazylimits.shared_resources.mixin.hotbars;

import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.registry.GameResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.HotbarManager;

@Mixin(HotbarManager.class)
public abstract class HotbarStorageMixin {
    /*? if <1.20.4 {*/
    /*@ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/io/File;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private File sharedresources$updateHotbarLoadPath(File file) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.HOTBARS, file.toPath()).toFile();
    }

    @ModifyArg(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"
            )
    )
    private File sharedresources$updateHotbarSavePath(File file) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.HOTBARS, file.toPath()).toFile();
    }
    *//*?} else {*/
    @ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/nio/file/Path;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private Path sharedresources$updateHotbarLoadPath(Path file) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.HOTBARS, file);
    }

    @ModifyArg(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V"
            )
    )
    private Path sharedresources$updateHotbarSavePath(Path file) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.HOTBARS, file);
    }
    /*?}*/
}
