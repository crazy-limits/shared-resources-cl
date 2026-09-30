package com.crazylimits.shared_resources;

import net.minecraft.resources.Identifier;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.api.GameResourceRegistry;
import com.crazylimits.shared_resources.platform.Platform;
import com.crazylimits.shared_resources.registry.GameResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.crazylimits.shared_resources.config.SharedResourcesConfig.CONFIG;

public class SharedResources {
	public static final String MODID = "shared-resources";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	/**
	 * Prepares the API and registers all resources, called once the config has loaded.
	 */
	public static void initApi() {
		GameResourceHelper.setConfigSource(CONFIG);

		// On Fabric our own resources come in through the entrypoint as well
		if (!Platform.isFabric()) {
			new GameResources().registerResources(GameResourceRegistry.REGISTRY);
		}
		Platform.getEntrypoints().forEach(entrypoint -> entrypoint.registerResources(GameResourceRegistry.REGISTRY));
		GameResourceRegistry.REGISTRY.finalise();
		CONFIG.initEnabledResources();
	}

	public static Identifier id(String path) {
		//? >=1.21 {
		return Identifier.fromNamespaceAndPath(MODID, path);
		//?} else {
		/*return new Identifier(MODID, path);
		*///?}
	}

	public static Identifier vanillaId(String path) {
		//? >=1.21 {
		return Identifier.withDefaultNamespace(path);
		//?} else {
		/*return new Identifier(path);
		 *///?}
	}
}
