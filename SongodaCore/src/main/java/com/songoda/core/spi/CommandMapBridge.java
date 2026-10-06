package com.songoda.core.spi;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

/**
 * Registers dynamically created {@link PluginCommand} instances on Paper-compatible servers.
 */
public interface CommandMapBridge {

    void register(Plugin plugin, PluginCommand command);
}
