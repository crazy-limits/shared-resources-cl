package nl.enjarai.shared_resources.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;
import nl.enjarai.shared_resources.api.GameResource;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import nl.enjarai.shared_resources.api.GameResourceRegistry;
import nl.enjarai.shared_resources.SharedResources;
import nl.enjarai.shared_resources.platform.Platform;
import nl.enjarai.shared_resources.config.serialization.GameDirectoryProviderAdapter;
import nl.enjarai.shared_resources.config.serialization.IdAdapter;
import nl.enjarai.shared_resources.registry.GameResources;
import nl.enjarai.shared_resources.util.directory.EmptyGameDirectoryProvider;
import nl.enjarai.shared_resources.util.directory.GameDirectoryProvider;
import nl.enjarai.shared_resources.util.directory.RootedGameDirectoryProvider;
import nl.enjarai.shared_resources.util.GameResourceConfig;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("unused")
public class SharedResourcesConfig implements GameResourceConfig {
    // Make sure we use the default config location instead of our modified one.
    public static final File CONFIG_FILE =
            Platform.getGameDir()
                    .resolve(GameResources.CONFIG.getDefaultPath()
                            .resolve(SharedResources.MODID + ".json")).toFile();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdAdapter())
            .registerTypeAdapter(GameDirectoryProvider.class, new GameDirectoryProviderAdapter())
            .setPrettyPrinting() // Makes the json use new lines instead of being a "one-liner"
            .disableHtmlEscaping() // We'll be able to use custom chars without them being saved differently
            .create();

    public static SharedResourcesConfig CONFIG;

    static {
        boolean isNew = !CONFIG_FILE.exists();

        CONFIG = loadConfigFile(CONFIG_FILE);

        SharedResources.initApi();

        // If this is a new config, we're not headless and not on Mac, we'll open the first time setup screen.
        if (isNew && !GraphicsEnvironment.isHeadless() && !System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            try {
                FirstStartupWindow.open(CONFIG);
            } catch (Exception e) {
                SharedResources.LOGGER.error("Error opening first startup window, skipping.", e);
            }
        }

        CONFIG.save();
    }

    /**
     * Forces the static initializer to run, loading the config and waking up the API.
     */
    public static void touch() {
    }

    /**
     * Whether the config screen can be shown, which requires Cloth Config.
     */
    public static boolean hasScreen() {
        return Platform.isModLoaded("cloth-config") || Platform.isModLoaded("cloth_config") || Platform.isModLoaded("cloth-config2");
    }

    public void initEnabledResources() {
        for (GameResource dir : GameResourceRegistry.REGISTRY) {
            Identifier id = dir.getId();

            if (!enabled.containsKey(id)) {
                setEnabled(id, dir.isDefaultEnabled());
            }
        }
    }

    // Actual saved data
    private GameDirectoryProvider globalDirectory = new RootedGameDirectoryProvider(Paths.get("."));
    private final HashMap<Identifier, Boolean> enabled = new HashMap<>();


    // Getters, setters and utils
    public GameDirectoryProvider getGlobalDirectory() {
        return globalDirectory;
    }

    public void setGlobalDirectory(@Nullable Path path) {
        if (path == null) {
            globalDirectory = new EmptyGameDirectoryProvider();
        } else {
            globalDirectory = new RootedGameDirectoryProvider(path);
        }

        for (GameResource resource : GameResourceRegistry.REGISTRY) {
            Path dirPath = GameResourceHelper.getPathOrDefaultFor(resource);
            resource.getUpdateCallback().onUpdate(dirPath);
        }
    }


    public boolean isEnabled(Identifier id) {
        if (globalDirectory instanceof EmptyGameDirectoryProvider) return false;

        return enabled.getOrDefault(id, false);
    }

    public void setEnabled(Identifier id, boolean enabled) {
        this.enabled.put(id, enabled);
    }

    @Override
    public boolean isEnabled(GameResource directory) {
        return isEnabled(directory.getId());
    }

    public HashMap<Identifier, Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public @Nullable Path getDirectory(GameResource resource) {
        return getGlobalDirectory().getDirectory(resource);
    }

    public void setEnabled(GameResource directory, boolean enabled) {
        setEnabled(directory.getId(), enabled);
    }



    public void save() {
        saveConfigFile(CONFIG_FILE);
    }

    /**
     * Loads config file.
     *
     * @param file file to load the config file from.
     * @return SharedResourcesConfig object
     */
    private static SharedResourcesConfig loadConfigFile(File file) {
        SharedResourcesConfig config = null;

        if (file.exists()) {
            // An existing config is present, we should use its values
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)
            )) {
                // Parses the config file and puts the values into config object
                config = GSON.fromJson(fileReader, SharedResourcesConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("Problem occurred when trying to load config: ", e);
            }
        }
        // gson.fromJson() can return null if file is empty
        if (config == null) {
            config = new SharedResourcesConfig();
        }

        return config;
    }

    /**
     * Saves the config to the given file.
     *
     * @param file file to save config to
     */
    private void saveConfigFile(File file) {
        file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
