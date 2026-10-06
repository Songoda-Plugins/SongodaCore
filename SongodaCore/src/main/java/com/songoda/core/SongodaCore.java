package com.songoda.core;

import org.bukkit.plugin.Plugin;

public final class SongodaCore {

    /**
     * Keeps {@link SongodaCorePluginLoader} reachable for bytecode (Paper loads it by name from {@code paper-plugin.yml};
     * without this, maven-shade {@code minimizeJar} in downstream plugins can strip that class).
     */
    @SuppressWarnings("unused")
    //private static final Class<?> PAPER_PLUGIN_LOADER = SongodaCorePluginLoader.class;

    private static Plugin currentPlugin;

    public static Plugin getPlugin() {
        return currentPlugin;
    }

    public static void setPlugin(Plugin plugin) {
        currentPlugin = plugin;
    }

}
