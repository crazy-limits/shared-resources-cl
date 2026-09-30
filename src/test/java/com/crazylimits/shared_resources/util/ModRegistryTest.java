package com.crazylimits.shared_resources.util;

import com.crazylimits.shared_resources.SharedResources;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModRegistryTest {
    private final Identifier first = SharedResources.id("first");
    private final Identifier second = SharedResources.id("second");

    @Test
    void registersAndLooksUpBothWays() {
        ModRegistry<String> registry = new ModRegistry<>();
        registry.register(first, "a");
        registry.register(second, "b");

        assertEquals("a", registry.get(first));
        assertEquals(second, registry.getId("b"));
        assertNull(registry.getId("c"));
        assertEquals(Set.of(first, second), registry.getIds());
    }

    @Test
    void lockedAfterFinalise() {
        ModRegistry<String> registry = new ModRegistry<>();
        registry.register(first, "a");
        registry.finalise();

        assertThrows(IllegalStateException.class, () -> registry.register(second, "b"));
        assertEquals("a", registry.get(first));
    }

    @Test
    void idsUseTheModNamespace() {
        assertEquals("shared-resources:first", first.toString());
    }
}
