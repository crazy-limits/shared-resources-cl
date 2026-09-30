package nl.enjarai.shared_resources.mixin.resourcepacks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import nl.enjarai.shared_resources.registry.GameResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;

@Mixin(PackSelectionScreen.class)
public abstract class PackScreenMixin {
    /**
     * Points "Open Pack Folder" and dropping packs onto the resource pack screen at the shared folder.
     * Matches every method since the button action is a lambda, whose name differs between loaders and versions.
     * The data pack screen uses a different folder and is left alone.
     */
    @ModifyExpressionValue(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/packs/PackSelectionScreen;packDir:Ljava/nio/file/Path;"
            )
    )
    private Path sharedresources$overrideOpenPackFolder(Path path) {
        if (!path.equals(Minecraft.getInstance().getResourcePackDirectory())) {
            return path;
        }
        return GameResourceHelper.getPathOrDefaultFor(GameResources.RESOURCEPACKS, path);
    }
}
