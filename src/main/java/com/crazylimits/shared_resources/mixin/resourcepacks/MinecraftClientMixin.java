package com.crazylimits.shared_resources.mixin.resourcepacks;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.registry.GameResources;
import com.crazylimits.shared_resources.util.ExternalFileResourcePackProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"
            )
    )
    private RepositorySource[] sharedresources$addResourcePackProvider(RepositorySource[] providers) {
        return ArrayUtils.add(providers, new ExternalFileResourcePackProvider(
                () -> GameResourceHelper.getPathFor(GameResources.RESOURCEPACKS)
        ));
    }
}
