package com.songoda.core.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface Hook {
    String getName();

    @NotNull String[] getPluginDependencies();

    default boolean canBeActivated() {
        for (String pluginName : getPluginDependencies()) {
            if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                return false;
            }
        }
        return true;
    }

    void activate(Plugin plugin);

    void deactivate();
}
