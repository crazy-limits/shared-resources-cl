package nl.enjarai.shared_resources.mixin.servers;

import nl.enjarai.shared_resources.api.GameResourceHelper;
import nl.enjarai.shared_resources.registry.GameResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.multiplayer.ServerList;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
    /*? if <1.20.4 {*/
    /*@Unique
    private File getOverwrittenPath(File original) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.SERVERS, original.toPath()).toFile();
    }

    @ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/io/File;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private File sharedresources$modifyFileLoad(File file) {
        return getOverwrittenPath(file);
    }

    @ModifyArg(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;"
            ),
            index = 2
    )
    private File sharedresources$modifyFileSave1(File file) {
        Path newPath = GameResourceHelper.getPathFor(GameResources.SERVERS);
        if (newPath != null) {
            return newPath.getParent().toFile();
        }
        return file;
    }

    @ModifyArgs(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/Util;safeReplaceFile(Ljava/io/File;Ljava/io/File;Ljava/io/File;)V"
            )
    )
    private void sharedresources$modifyFileSave2(Args args) {
        args.set(0, getOverwrittenPath(args.get(0)));

        Path newPath = GameResourceHelper.getPathFor(GameResources.SERVERS);
        if (newPath != null) {
            args.set(2, new File(newPath.toFile() + "_old"));
        }
    }
    *//*?} else {*/
    @Unique
    private Path getOverwrittenPath(Path original) {
        return GameResourceHelper.getPathOrDefaultFor(GameResources.SERVERS, original);
    }

    @ModifyArg(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/nio/file/Path;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private Path sharedresources$modifyFileLoad(Path file) {
        return getOverwrittenPath(file);
    }

    @ModifyArg(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Files;createTempFile(Ljava/nio/file/Path;Ljava/lang/String;Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;"
            ),
            index = 0
    )
    private Path sharedresources$modifyFileSave1(Path file) {
        Path newPath = GameResourceHelper.getPathFor(GameResources.SERVERS);
        if (newPath != null) {
            return newPath.getParent();
        }
        return file;
    }

    @ModifyArgs(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21.11 {
                    target = "Lnet/minecraft/util/Util;safeReplaceFile(Ljava/nio/file/Path;Ljava/nio/file/Path;Ljava/nio/file/Path;)V"
                    //?} else
                    //target = "Lnet/minecraft/Util;safeReplaceFile(Ljava/nio/file/Path;Ljava/nio/file/Path;Ljava/nio/file/Path;)V"
            )
    )
    private void sharedresources$modifyFileSave2(Args args) {
        args.set(0, getOverwrittenPath(args.get(0)));

        Path newPath = GameResourceHelper.getPathFor(GameResources.SERVERS);
        if (newPath != null) {
            args.set(2, newPath.resolveSibling(newPath.getFileName() + "_old"));
        }
    }
    /*?}*/
}
