package com.crazylimits.shared_resources.gui;

//? if cloth {

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import com.crazylimits.shared_resources.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import com.crazylimits.shared_resources.versioned.TextBuilder;
//? if >=26.3 {
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.system.MemoryUtil;
//?} else
//import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class DirectoryConfigEntry extends AbstractConfigListEntry<Path> {

    private final AtomicReference<Path> path;
    private final Path initialValue;
    private final Path defaultValue;
    private final Consumer<Path> saveConsumer;

    private final Button pathButton;
    private final Button resetButton;
    private final List<AbstractWidget> widgets;

    public DirectoryConfigEntry(Component fieldName, Path initialValue, Path defaultValue, Consumer<Path> saveConsumer) {
        super(fieldName, false);
        this.path = new AtomicReference<>(initialValue);
        this.initialValue = initialValue;
        this.defaultValue = defaultValue;
        this.saveConsumer = saveConsumer;

        pathButton = Button.builder(TextBuilder.translatable("config.shared_resources.directoryEntry"), button -> {
            Path absolutePath = path.get().isAbsolute() ? path.get() : Platform.getGameDir().resolve(path.get());
            pickFolder(absolutePath, path::set);
        }).bounds(0, 0, 150, 20).build();
        Component resetButtonKey = TextBuilder.translatable("text.cloth-config.reset_value");
        resetButton = Button.builder(resetButtonKey, (widget) -> path.set(defaultValue))
                .bounds(0, 0, Minecraft.getInstance().font.width(resetButtonKey) + 6, 20)
                .build();
        widgets = Lists.newArrayList(pathButton, resetButton);
    }

    @Override
    public boolean isEdited() {
        return super.isEdited() || !path.get().equals(initialValue);
    }

    @Override
    public boolean isRequiresRestart() { // TODO check if entries need restart
        return true;
    }

    @Override
    public Path getValue() {
        return path.get();
    }

    @Override
    public Optional<Path> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    @Override
    public void save() {
        if (saveConsumer != null) {
            saveConsumer.accept(path.get());
        }
    }

    @Override
    //? if >=26.1 {
    public void extractRenderState(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    //?} else {
    /*public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    *///?}

        pathButton.setX(x);
        pathButton.setY(y);
        pathButton.active = isEditable();
        pathButton.setWidth(entryWidth - resetButton.getWidth() - 2);
        pathButton.setMessage(Component.literal(truncateStart(path.get().toString(), (int) (pathButton.getWidth() * 0.9))));

        resetButton.setX(x + entryWidth - resetButton.getWidth());
        resetButton.setY(y);
        resetButton.active = isEditable() && getDefaultValue().isPresent() && !defaultValue.equals(path.get());

        for (AbstractWidget widget : widgets) {
            //? if >=26.1 {
            widget.extractRenderState(context, mouseX, mouseY, delta);
            //?} else
            //widget.render(context, mouseX, mouseY, delta);
        }
    }

    //? if >=26.3 {
    // Kept alive until the next dialog, SDL calls back after the dialog closes
    private static SDL_DialogFileCallback dialogCallback;
    //?}

    /**
     * Opens the system folder picker. Minecraft 26.3 moved from GLFW to SDL and no longer ships tinyfd.
     */
    private static void pickFolder(Path start, Consumer<Path> onPicked) {
        //? if >=26.3 {
        if (dialogCallback != null) dialogCallback.free();
        dialogCallback = SDL_DialogFileCallback.create((userdata, fileList, filter) -> {
            // A null list means an error, a list starting with null means the dialog was cancelled
            if (fileList == 0) return;
            long first = MemoryUtil.memGetAddress(fileList);
            if (first != 0) {
                Path picked = Paths.get(MemoryUtil.memUTF8(first));
                Minecraft.getInstance().execute(() -> onPicked.accept(picked));
            }
        });
        SDLDialog.SDL_ShowOpenFolderDialog(dialogCallback, 0, 0, start.toString(), false);
        //?} else {
        /*String val = TinyFileDialogs.tinyfd_selectFolderDialog("Select directory", start.toString());
        if (val != null) {
            onPicked.accept(Paths.get(val));
        }
        *///?}
    }

    /**
     * Cuts text off at the front when it doesn't fit, the end of a path is the interesting part.
     */
    private static String truncateStart(String text, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return "..." + font.plainSubstrByWidth(text, maxWidth - font.width("..."), true);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }
}
//?}
