package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class HookManager<T extends OutdatedHookInterface> {
    private final Class typeClass;
    private T defaultHook = null;
    private boolean loaded = false;
    private final Map<PluginHook, T> registeredHooks = new HashMap<>();

    public HookManager(Class typeClass) {
        this.typeClass = typeClass;
    }

    /**
     * Load all supported plugins.
     */
    public void load() {
        load(null);
    }

    /**
     * Load all supported plugins.
     *
     * @param hookingPlugin plugin to pass to the hook handler
     */
    public void load(Plugin hookingPlugin) {
        if (!loaded) {
            registeredHooks.putAll(PluginHook.loadHooks(typeClass, hookingPlugin).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (T) e.getValue())));

            if (!registeredHooks.isEmpty()) {
                defaultHook = registeredHooks.values().iterator().next();
            }

            loaded = true;
        }
    }

    /**
     * Get the currently selected plugin hook. <br>
     * If none were set, then the first one found is used.
     *
     * @return The instance of T that was created, or null if none available.
     */
    public T getCurrentHook() {
        return defaultHook;
    }

    /**
     * Set the default hook to a different plugin, if that plugin exists. <br>
     * If the plugin is not loaded or supported,
     * the previously defined default will be used.
     *
     * @param name name of the plugin to use
     *
     * @return true if the default was set to this plugin
     */
    public boolean setPreferredHook(String name) {
        T hook = getHook(name);
        if (hook != null) {
            defaultHook = hook;

            return true;
        }

        return false;
    }

    /**
     * Set the default hook to a different plugin, if that plugin exists.  <br />
     * If the plugin is not loaded or supported,
     * the previously defined default will be used.
     *
     * @param plugin plugin to use
     *
     * @return true if the default was set to this plugin
     */
    public boolean setPreferredHook(PluginHook plugin) {
        T hook = getHook(plugin);
        if (hook != null) {
            defaultHook = hook;

            return true;
        }

        return false;
    }

    /**
     * Try to grab the handler for this specific plugin hook.
     *
     * @param name plugin to use
     *
     * @return returns null if plugin is not enabled
     */
    public T getHook(String name) {
        if (name == null) {
            return null;
        }

        final String plugin = name.trim();
        return registeredHooks.get(registeredHooks.keySet().stream()
                .filter(type -> type.plugin.equalsIgnoreCase(plugin))
                .findFirst().orElse(null));
    }

    /**
     * Try to grab the handler for this specific plugin hook.
     *
     * @param hook plugin to use
     *
     * @return returns null if plugin is not enabled
     */
    public T getHook(PluginHook hook) {
        return registeredHooks.get(hook);
    }

    /**
     * Grab a list of all supported and loaded plugin hooks.
     *
     * @return an immutable collection of the loaded handler instances
     */
    public Collection<T> getRegisteredHooks() {
        return Collections.unmodifiableCollection(registeredHooks.values());
    }

    /**
     * Grab a list of all supported and loaded plugin hooks.
     *
     * @return an immutable collection of plugin names that are loaded.
     */
    public List<String> getRegisteredPlugins() {
        return registeredHooks.keySet().stream()
                .map(v -> v.plugin)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all supported plugins that we can hook into.
     *
     * @return an immutable collection of plugin names that can be used.
     */
    public List<String> getPossiblePlugins() {
        return PluginHook.getHooks(typeClass).stream()
                .map(v -> v.plugin)
                .collect(Collectors.toList());
    }

    /**
     * Check to see if a specific plugin hook is enabled.
     *
     * @param name plugin to check
     *
     * @return true if this plugin is supported and loaded
     */
    public boolean isEnabled(String name) {
        return getHook(name) != null;
    }

    /**
     * Check to see if a specific plugin hook is enabled.
     *
     * @param hook plugin to check
     *
     * @return true if this plugin is supported and loaded
     */
    public boolean isEnabled(PluginHook hook) {
        return registeredHooks.containsKey(hook);
    }

    /**
     * Check to see if there is a default hook loaded.
     *
     * @return returns false if there are no supported plugins loaded
     */
    public boolean isEnabled() {
        return defaultHook != null;
    }

    /**
     * Get the name of the default plugin being hooked into.
     *
     * @return plugin name, or null if none enabled.
     */
    public String getName() {
        return defaultHook != null ? defaultHook.getName() : null;
    }
}
