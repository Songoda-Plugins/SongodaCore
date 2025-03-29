package com.songoda.core.hooks;

/**
 * This interface is part of the old hook system and is being replaced
 */
@Deprecated
public interface OutdatedHookInterface {
    /**
     * Get the name of the plugin being used
     */
    String getName();

    /**
     * Check to see if the economy plugin being used is active
     *
     * @return true if the plugin is loaded and active
     */
    boolean isEnabled();
}
