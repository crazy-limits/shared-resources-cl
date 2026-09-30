//? if fabric {
package nl.enjarai.shared_resources;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import nl.enjarai.shared_resources.config.SharedResourcesConfig;
import nl.enjarai.shared_resources.registry.GameResources;

import java.lang.reflect.Field;
import java.nio.file.Path;

public class SharedResourcesPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // Carefully touch our config to get it to load and wake up the API
        SharedResourcesConfig.touch();

        // Load config directory override if enabled.
        Path configDir = GameResourceHelper.getPathFor(GameResources.CONFIG);

        if (configDir != null) {

            SharedResources.LOGGER.info("Config directory override enabled, changing fabric config directory to: {}. *proceed with caution*", configDir);

            try {
                Class<?> loaderClass;

                if (FabricLoader.getInstance().isModLoaded("quilt_loader")) {
                    loaderClass = Class.forName("org.quiltmc.loader.impl.QuiltLoaderImpl");
                } else if (FabricLoader.getInstance().isModLoaded("fabricloader")) {
                    loaderClass = Class.forName("net.fabricmc.loader.impl.FabricLoaderImpl");
                } else {
                    throw new IllegalStateException("Could not find Fabric or Quilt. Abort setting config override");
                }

                Field configDirField = loaderClass.getDeclaredField("configDir");
                configDirField.setAccessible(true);
                configDirField.set(loaderClass.getDeclaredField("INSTANCE").get(null), configDir);

            } catch ( IllegalStateException |
                      IllegalAccessException |
                      NoSuchFieldException |
                      ClassNotFoundException e) {
                SharedResources.LOGGER.error("Failed to set config directory override.", e);

            }
        }
    }
}
//?}
