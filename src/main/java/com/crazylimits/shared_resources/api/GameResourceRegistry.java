package com.crazylimits.shared_resources.api;

import com.crazylimits.shared_resources.util.ModRegistry;

/**
 * Register resource directories here.
 * See {@link ResourceDirectoryBuilder} on how to create a custom resource directory.
 */
public class GameResourceRegistry extends ModRegistry<GameResource> {
    public static final GameResourceRegistry REGISTRY = new GameResourceRegistry();

    /**
     * Finalise the registry and run related callbacks.
     * This should be called after all resources have been registered.
     * <b>Do not call this method! The main Shared Resources mod should take care of it.</b>
     */
    @Override
    public void finalise() {
        DefaultGameResources.load(this);
        super.finalise();
    }
}
