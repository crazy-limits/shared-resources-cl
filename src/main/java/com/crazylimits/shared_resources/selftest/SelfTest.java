package com.crazylimits.shared_resources.selftest;

import com.crazylimits.shared_resources.SharedResources;
import com.crazylimits.shared_resources.api.GameResource;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.platform.Platform;
import com.crazylimits.shared_resources.registry.GameResources;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Checks every feature in a running game and writes a report, then quits.
 * Only active when the {@code shared-resources.selftest} system property names the report file,
 * which the {@code selfTest} Gradle tasks set up along with a prepared shared directory.
 */
public final class SelfTest {
    public static final String PROPERTY = "shared-resources.selftest";
    // Written into the shared options.txt before launch, it must survive the game saving its options
    public static final String KEPT_OPTION = "sharedResourcesSelfTest";
    private static final int SETTLE_TICKS = 40;
    private static final int SCREENSHOT_TIMEOUT_TICKS = 400;
    private static final long WATCHDOG_MILLIS = 10 * 60 * 1000;

    private static final String REPORT = System.getProperty(PROPERTY);
    private static State state = State.LOADING;
    private static int ticks = 0;
    private static final List<Result> results = new ArrayList<>();

    private enum State { LOADING, SETTLING, SCREENSHOT, DONE }

    private SelfTest() {
    }

    public static boolean isEnabled() {
        return REPORT != null;
    }

    /**
     * Called at the start of every client tick while the self-test is enabled.
     */
    public static void tick(Minecraft minecraft) {
        try {
            switch (state) {
                case LOADING:
                    startWatchdog();
                    if (isLoaded(minecraft)) state = State.SETTLING;
                    break;
                case SETTLING:
                    if (++ticks >= SETTLE_TICKS) {
                        runChecks(minecraft);
                        takeScreenshot(minecraft);
                        ticks = 0;
                        state = State.SCREENSHOT;
                    }
                    break;
                case SCREENSHOT:
                    Path dir = shared(GameResources.SCREENSHOTS);
                    if (hasPng(dir)) {
                        results.add(Result.pass("screenshots", "saved to " + dir));
                    } else if (++ticks < SCREENSHOT_TIMEOUT_TICKS) {
                        break;
                    } else {
                        results.add(Result.fail("screenshots", "no screenshot appeared in " + dir));
                    }
                    finish(minecraft, null);
                    break;
                case DONE:
                    break;
            }
        } catch (Throwable e) {
            finish(minecraft, e);
        }
    }

    private static boolean isLoaded(Minecraft minecraft) {
        //? if >=1.21 {
        return minecraft.isGameLoadFinished();
        //?} else
        /*return minecraft.getOverlay() == null;*/
    }

    private static void runChecks(Minecraft minecraft) {
        check("options", () -> {
            minecraft.options.save();
            Path file = shared(GameResources.OPTIONS);
            String options = Files.readString(file, StandardCharsets.UTF_8);
            require(options.contains("fov:"), file + " wasn't written by the game");
            require(options.contains(KEPT_OPTION + ":"), "unknown option " + KEPT_OPTION + " was dropped from " + file);
            return "saved to " + file + ", unknown options kept";
        });

        check("saves", () -> {
            Path expected = shared(GameResources.SAVES);
            Path actual = minecraft.getLevelSource().getBaseDir();
            require(same(expected, actual), "worlds are read from " + actual);
            return "worlds read from " + actual;
        });

        check("resourcepacks", () -> {
            Path dir = shared(GameResources.RESOURCEPACKS);
            writePack(dir.resolve("selftest_pack"));
            PackRepository repository = minecraft.getResourcePackRepository();
            repository.reload();
            require(repository.getAvailableIds().stream().anyMatch(id -> id.contains("selftest_pack")),
                    "pack in " + dir + " isn't listed, available: " + repository.getAvailableIds());
            return "pack from " + dir + " is listed";
        });

        check("datapacks", () -> {
            Path dir = shared(GameResources.DATAPACKS);
            writePack(dir.resolve("selftest_datapack"));
            Path worldPacks = Files.createTempDirectory("shared-resources-selftest");
            //? if >=1.20.2 {
            PackRepository repository = ServerPacksSource.createPackRepository(worldPacks, minecraft.directoryValidator());
            //?} else
            /*PackRepository repository = ServerPacksSource.createPackRepository(worldPacks);*/
            repository.reload();
            require(repository.getAvailableIds().stream().anyMatch(id -> id.contains("selftest_datapack")),
                    "data pack in " + dir + " isn't offered to new worlds, available: " + repository.getAvailableIds());
            return "data pack from " + dir + " is offered to new worlds";
        });

        check("servers", () -> {
            Path file = shared(GameResources.SERVERS);
            ServerList servers = new ServerList(minecraft);
            servers.load();
            servers.save();
            require(Files.isRegularFile(file), "server list wasn't saved to " + file);
            return "saved to " + file;
        });

        check("hotbars", () -> {
            Path file = shared(GameResources.HOTBARS);
            minecraft.getHotbarManager().save();
            require(Files.isRegularFile(file), "saved hotbars weren't written to " + file);
            return "saved to " + file;
        });

        //? if fabric {
        check("config", () -> {
            Path expected = shared(GameResources.CONFIG);
            Path actual = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
            require(same(expected, actual), "Fabric's config directory is " + actual);
            return "Fabric's config directory is " + actual;
        });
        //?}
    }

    private static void takeScreenshot(Minecraft minecraft) {
        //? if >=26.2 {
        Screenshot.grab(minecraft, false);
        //?} else
        /*Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> {});*/
    }

    // --- Helpers ---

    private static Path shared(GameResource resource) {
        Path path = GameResourceHelper.getPathFor(resource);
        require(path != null, resource.getDefaultPath() + " isn't shared, is the self-test config in place?");
        return path;
    }

    private static void writePack(Path dir) throws IOException {
        Files.createDirectories(dir);
        // Lenient metadata that every version from 1.20.1 to 26.x accepts
        Files.writeString(dir.resolve("pack.mcmeta"), "{\"pack\": {"
                + "\"description\": \"Shared Resources self-test\", \"pack_format\": 15, "
                + "\"supported_formats\": [0, 1000], \"min_format\": 0, \"max_format\": 1000}}", StandardCharsets.UTF_8);
    }

    private static boolean hasPng(Path dir) {
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(file -> file.toString().endsWith(".png"));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean same(Path a, Path b) throws IOException {
        return Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void check(String name, Callable<String> check) {
        try {
            results.add(Result.pass(name, check.call()));
        } catch (Throwable e) {
            results.add(Result.fail(name, e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private static void startWatchdog() {
        if (ticks++ > 0) return;
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(WATCHDOG_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            results.add(Result.fail("watchdog", "the game didn't finish loading within " + WATCHDOG_MILLIS / 1000 + "s"));
            writeReport();
            Runtime.getRuntime().halt(1);
        }, "Shared Resources self-test watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void finish(Minecraft minecraft, Throwable error) {
        state = State.DONE;
        if (error != null) {
            results.add(Result.fail("self-test", error.toString()));
        }
        writeReport();
        minecraft.stop();
    }

    private static synchronized void writeReport() {
        JsonObject report = new JsonObject();
        report.addProperty("loader", Platform.LOADER);
        JsonArray checks = new JsonArray();
        for (Result result : results) {
            JsonObject check = new JsonObject();
            check.addProperty("name", result.name);
            check.addProperty("passed", result.passed);
            check.addProperty("detail", result.detail);
            checks.add(check);
            SharedResources.LOGGER.info("Self-test {} {}: {}", result.passed ? "PASS" : "FAIL", result.name, result.detail);
        }
        report.add("checks", checks);
        try {
            Path file = Paths.get(REPORT);
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SharedResources.LOGGER.error("Couldn't write the self-test report", e);
        }
    }

    private static final class Result {
        final String name;
        final boolean passed;
        final String detail;

        private Result(String name, boolean passed, String detail) {
            this.name = name;
            this.passed = passed;
            this.detail = detail;
        }

        static Result pass(String name, String detail) {
            return new Result(name, true, detail);
        }

        static Result fail(String name, String detail) {
            return new Result(name, false, detail);
        }
    }
}
