package com.crazylimits.shared_resources.forge;

//? if forge {
/*import net.minecraftforge.fml.common.Mod;
import com.crazylimits.shared_resources.config.SharedResourcesConfig;
//? if <1.21.5 {
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import com.crazylimits.shared_resources.config.SharedResourcesConfigScreen;
//?}

@Mod("shared_resources")
public class SharedResourcesForge {
    public SharedResourcesForge() {
        SharedResourcesConfig.touch();

        // Cloth Config only exists for Forge up to 1.21.4
        //? if <1.21.5 {
        if (SharedResourcesConfig.hasScreen()) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (client, parent) -> SharedResourcesConfigScreen.create(SharedResourcesConfig.CONFIG, parent)));
        }
        //?}
    }
}
*///?}
