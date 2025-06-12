package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.hologram.HologramHook;
import com.songoda.core.hooks.holograms.Holograms;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

/**
 * A convenience class for static access to a Holograms HookManager
 *
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()} and {@link HologramHook}
 */
@Deprecated
public class HologramManager {
    private static final HookManager<Holograms> manager = new HookManager(Holograms.class);

    /**
     * Load all supported economy plugins.<br/>
     * Note: This method should be called in your plugin's onEnable() section
     *
     * @param plugin plugin that will be using the holograms
     */
    public static void load(Plugin plugin) {
        manager.load(plugin);
    }

    public static HookManager getManager() {
        return manager;
    }

    /**
     * Check to see if there is a default holograms hook loaded. <br />
     * NOTE: using a default hologram assumes that this library is shaded
     *
     * @return returns false if there are no supported hologram plugins
     */
    public static boolean isEnabled() {
        return manager.isEnabled();
    }

    /**
     * Grab the default hologram plugin. <br />
     * NOTE: using a default hologram assumes that this library is shaded
     *
     * @return returns null if no plugin enabled
     */
    public static Holograms getHolograms() {
        return manager.getCurrentHook();
    }

    public static void createHologram(String id, Location location, String line) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().createHologram(id, location, line);
        }
    }

    public static void createHologram(String id, Location location, List<String> lines) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().createHologram(id, location, lines);
        }
    }

    public static void removeHologram(String id) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().removeHologram(id);
        }
    }

    public static void removeAllHolograms() {
        if (manager.isEnabled()) {
            manager.getCurrentHook().removeAllHolograms();
        }
    }

    public static void updateHologram(String id, String line) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().updateHologram(id, line);
        }
    }

    public static void updateHologram(String id, List<String> lines) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().updateHologram(id, lines);
        }
    }

    public static void bulkUpdateHolograms(Map<String, List<String>> holograms) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().bulkUpdateHolograms(holograms);
        }
    }

    public static boolean isHologramLoaded(String id) {
        if (manager.isEnabled()) {
            return manager.getCurrentHook().isHologramLoaded(id);
        }

        return false;
    }
}
