package com.crazylimits.shared_resources.mixin.selftest;

import com.crazylimits.shared_resources.selftest.SelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link SelfTest}, does nothing unless the self-test is switched on.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSelfTestMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void sharedresources$selfTest(CallbackInfo ci) {
        if (SelfTest.isEnabled()) {
            SelfTest.tick((Minecraft) (Object) this);
        }
    }
}
