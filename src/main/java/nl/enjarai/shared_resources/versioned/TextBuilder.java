package nl.enjarai.shared_resources.versioned;

import net.minecraft.network.chat.Component;

public interface TextBuilder {
    static Component translatable(String key, Object... objects) {
        return Component.translatable(key, objects);
    }
}
