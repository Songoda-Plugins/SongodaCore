package com.songoda.core;

import com.songoda.core.hooks.HookRegistry;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.compatibility.KnownServerVersions;
import org.jetbrains.annotations.NotNull;

/**
 * Base plugin class for modern Songoda plugins.
 */
public abstract class SongodaPlugin extends VortexPlugin {

    private final HookRegistry hookRegistry = new HookRegistry();

    /**
     * Returns the active Songoda plugin instance.
     *
     * @return the active Songoda plugin
     * @throws IllegalStateException if called before Bukkit invokes onLoad
     */
    public static SongodaPlugin getInstance() {
        return (SongodaPlugin) VortexPlugin.getInstance();
    }

    /**
     * Returns the registry used to run optional plugin integrations safely.
     *
     * @return the hook registry
     */
    public final HookRegistry getHookRegistry() {
        return this.hookRegistry;
    }

    /**
     * Returns SongodaCore's baseline Minecraft version. Individual plugins may
     * override this with a newer minimum.
     *
     * @return the minimum supported server version
     */
    @Override
    protected @NotNull KnownServerVersions getMinimumServerVersion() {
        return MINIMUM_SUPPORTED_SERVER_VERSION;
    }
}
