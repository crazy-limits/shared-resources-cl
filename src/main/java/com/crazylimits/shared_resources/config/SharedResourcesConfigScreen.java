package com.crazylimits.shared_resources.config;

//? if cloth {

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import com.crazylimits.shared_resources.api.GameResource;
import com.crazylimits.shared_resources.api.GameResourceHelper;
import com.crazylimits.shared_resources.api.GameResourceRegistry;
import com.crazylimits.shared_resources.compat.CompatMixinErrorHandler;
import com.crazylimits.shared_resources.gui.DirectoryConfigEntry;
import com.crazylimits.shared_resources.util.directory.RootedGameDirectoryProvider;
import com.crazylimits.shared_resources.versioned.TextBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the Cloth Config screen. Only load this class after checking {@link SharedResourcesConfig#hasScreen()},
 * Cloth Config is an optional dependency.
 */
public final class SharedResourcesConfigScreen {
    private SharedResourcesConfigScreen() {
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Screen create(SharedResourcesConfig config, Screen parent) {

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TextBuilder.translatable("config.shared_resources.title"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Path globalDir = null;
        if (config.getGlobalDirectory() instanceof RootedGameDirectoryProvider) {
            globalDir = ((RootedGameDirectoryProvider) config.getGlobalDirectory()).getRoot();
        }

        ConfigCategory generalCategory = builder
                .getOrCreateCategory(TextBuilder.translatable("config.shared_resources.general"))
                .addEntry(
                        entryBuilder.startTextDescription(
                                TextBuilder.translatable("config.shared_resources.general.directory")
                        ).build()
                )
                .addEntry(new DirectoryConfigEntry(
                        TextBuilder.translatable("config.shared_resources.general.directory"),
                        globalDir,
                        Paths.get("global_resources"),
                        config::setGlobalDirectory
                ))
                .addEntry(
                        entryBuilder.startTextDescription(
                                TextBuilder.translatable("config.shared_resources.general.enabled")
                        ).build()
                );

        List<Identifier> resources = new ArrayList<>(GameResourceRegistry.REGISTRY.getIds());
        Collections.sort(resources);
        for (Identifier id : resources) {
            GameResource resource = GameResourceRegistry.REGISTRY.get(id);
            boolean enabled = config.isEnabled(id);
            boolean failed = resource.getMixinPackages().stream().anyMatch(CompatMixinErrorHandler::hasFailed);

            List<Component> description = new ArrayList<>(resource.getDescription());
            if (resource.isExperimental()) {
                if (!description.isEmpty()) description.add(Component.nullToEmpty(" "));
                description.add(TextBuilder.translatable("config.shared_resources.experimental[0]"));
                description.add(TextBuilder.translatable("config.shared_resources.experimental[1]"));
            }
            if (failed) {
                if (!description.isEmpty()) description.add(Component.nullToEmpty(" "));
                description.add(TextBuilder.translatable("config.shared_resources.failed[0]"));
                description.add(TextBuilder.translatable("config.shared_resources.failed[1]"));
                description.add(TextBuilder.translatable("config.shared_resources.failed[2]"));
            }

            Component displayName = resource.getDisplayName();
            if (failed) {
                displayName = displayName.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            }

            BooleanListEntry entry = entryBuilder.startBooleanToggle(displayName, enabled)
                    .setDefaultValue(resource.isDefaultEnabled())
                    .setSaveConsumer(newEnabled -> {
                        config.setEnabled(id, newEnabled);
                        resource.getUpdateCallback().onUpdate(GameResourceHelper.getPathFor(resource));
                    })
                    .setTooltip(description.toArray(new Component[0]))
                    .setRequirement(() -> !failed)
                    .build();
            entry.setRequiresRestart(resource.isRequiresRestart());
            generalCategory.addEntry(entry);
        }

        return builder.build();
    }
}
//?}
