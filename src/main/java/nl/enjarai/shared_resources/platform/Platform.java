package nl.enjarai.shared_resources.platform;

import nl.enjarai.shared_resources.api.SharedResourcesEntrypoint;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Mod loader specific functionality, so the rest of the mod doesn't have to care which loader it runs on.
 */
public final class Platform {
    //? if fabric {
    public static final String LOADER = "fabric";
    //?} elif neoforge {
    /*public static final String LOADER = "neoforge";
    *///?} else {
    /*public static final String LOADER = "forge";
    *///?}

    private Platform() {
    }

    public static boolean isFabric() {
        return LOADER.equals("fabric");
    }

    public static Path getGameDir() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        //?} elif neoforge {
        /*return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
        *///?} else {
        /*return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
        *///?}
    }

    /**
     * Safe to call from mixin config plugins, before mods are constructed.
     */
    public static boolean isModLoaded(String id) {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
        //?} elif neoforge {
        /*net.neoforged.fml.loading.LoadingModList mods =
                //? if >=1.21.9 {
                net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList();
                //?} else
                /^net.neoforged.fml.loading.FMLLoader.getLoadingModList();^/
        return mods.getModFileById(id) != null;
        *///?} else {
        /*//? if >=26.1 {
        return net.minecraftforge.fml.loading.LoadingModList.getModFileById(id) != null;
        //?} else
        /^return net.minecraftforge.fml.loading.LoadingModList.get().getModFileById(id) != null;^/
        *///?}
    }

    /**
     * Finds all API entrypoints. On Fabric these are declared as the `shared-resources` entrypoint,
     * on (Neo)Forge they are Java services in `META-INF/services/nl.enjarai.shared_resources.api.SharedResourcesEntrypoint`.
     */
    public static List<SharedResourcesEntrypoint> getEntrypoints() {
        List<SharedResourcesEntrypoint> entrypoints = new ArrayList<>();
        //? if fabric {
        entrypoints.addAll(net.fabricmc.loader.api.FabricLoader.getInstance()
                .getEntrypoints("shared-resources", SharedResourcesEntrypoint.class));
        //?} else {
        /*ServiceLoader.load(SharedResourcesEntrypoint.class, SharedResourcesEntrypoint.class.getClassLoader())
                .forEach(entrypoints::add);
        *///?}
        return entrypoints;
    }
}
