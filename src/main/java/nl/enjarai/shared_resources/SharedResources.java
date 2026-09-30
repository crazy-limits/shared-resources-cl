package nl.enjarai.shared_resources;

import net.minecraft.resources.Identifier;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import nl.enjarai.shared_resources.api.GameResourceRegistry;
import nl.enjarai.shared_resources.platform.Platform;
import nl.enjarai.shared_resources.registry.GameResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.enjarai.shared_resources.config.SharedResourcesConfig.CONFIG;

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
