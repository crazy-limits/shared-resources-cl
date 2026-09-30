package com.crazylimits.shared_resources.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import com.crazylimits.shared_resources.config.SharedResourcesConfig;
import com.crazylimits.shared_resources.config.SharedResourcesConfigScreen;

@Mod(value = "shared_resources", dist = Dist.CLIENT)
public class SharedResourcesNeoForge {
    public SharedResourcesNeoForge(ModContainer container) {
        SharedResourcesConfig.touch();

        if (SharedResourcesConfig.hasScreen()) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (IConfigScreenFactory) (mod, parent) -> SharedResourcesConfigScreen.create(SharedResourcesConfig.CONFIG, parent));
        }
    }
}
*///?}
