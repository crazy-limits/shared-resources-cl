package com.crazylimits.shared_resources.mixin.saves;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.registry.GameResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @ModifyExpressionValue(
            method = "<init>",
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "stringValue=saves"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;",
                    ordinal = 0
            )
    )
    private Path sharedresources$changePath(Path original) {
        Path newPath = GameResourceHelper.getPathFor(GameResources.SAVES);

        if (newPath != null) {
            return newPath;
        }

        return original;
    }
}
