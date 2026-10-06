package com.songoda.core.hooks.plugin;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

@Setter
@Getter
public abstract class PluginHook {

    protected boolean enabled = false;

    public PluginHook() {

    }

    public abstract String getRequiredPlugin();

    public boolean canEnable() {
        return Bukkit.getPluginManager().isPluginEnabled(getRequiredPlugin());
    }

    public abstract void onEnable();

    public abstract void onDisable();

    protected @Nullable <T extends Plugin> T getPlugin(Class<T> pluginClass) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(getRequiredPlugin());
        if (plugin == null) {
            System.err.println(getRequiredPlugin() + " is not enabled, cannot get plugin instance.");
            return null;
        }
        if (!pluginClass.isAssignableFrom(plugin.getClass())) {
            System.err.println(getRequiredPlugin() + " is not of type " + pluginClass.getName() + ", cannot cast. Required: " + plugin.getClass().getName());
            return null;
        }
        return pluginClass.cast(plugin);
    }
}
