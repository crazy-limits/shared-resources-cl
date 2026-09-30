package com.crazylimits.shared_resources.mixin.screenshots;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Screenshot;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.platform.Platform;
import com.crazylimits.shared_resources.registry.GameResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

@Mixin(Screenshot.class)
public abstract class ScreenshotRecorderMixin {
    /**
     * Swaps out `new File(gameDirectory, "screenshots")`. Matches every method since the folder
     * is created in a lambda on newer versions, which is named differently per loader and version.
     */
    @WrapOperation(
            method = "*",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;"
            )
    )
    private static File sharedresources$modScreenshotDir(File parent, String child, Operation<File> original) {
        File file = original.call(parent, child);
        if (!child.equals("screenshots") || Platform.isModLoaded("memories-are-all-we-have")) {
            return file;
        }

        return GameResourceHelper.getPathOrDefaultFor(GameResources.SCREENSHOTS, file.toPath()).toFile();
    }
}
