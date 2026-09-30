package com.crazylimits.shared_resources.api;

/**
 * Entrypoint interface for the Shared Resources mod.
 * Use this to register custom resource directories without a hard dependency.
 * <b>Gets run very early</b> (a preLaunch entrypoint on Fabric), limit interactions with vanilla classes as much as possible.
 * <p>
 * On Fabric, register implementations as the {@code shared-resources} entrypoint in {@code fabric.mod.json}.
 * On NeoForge and Forge, register them as a Java service in
 * {@code META-INF/services/com.crazylimits.shared_resources.api.SharedResourcesEntrypoint}.
 */
public interface SharedResourcesEntrypoint {
    void registerResources(GameResourceRegistry registry);
}
