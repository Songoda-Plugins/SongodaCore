package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.log.Log;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

/**
 * A convenience class for static access to a Log HookManager
 *
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class LogManager {
    private static final HookManager<Log> manager = new HookManager(Log.class);

    /**
     * Load all supported log plugins. <br />
     * Note: This method should be called in your plugin's onEnable() section
     */
    public static void load() {
        manager.load();
    }

    public static HookManager getManager() {
        return manager;
    }

    /**
     * Grab the default log plugin. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @return returns null if no plugin enabled
     */
    public static Log getLog() {
        return manager.getCurrentHook();
    }

    /**
     * Check to see if there is a default log loaded. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @return returns false if there are no supported log plugins
     */
    public static boolean isEnabled() {
        return manager.isEnabled();
    }

    /**
     * Get the name of the log plugin being used. <br />
     * NOTE: using a default log assumes that this library is shaded
     */
    public static String getName() {
        return manager.getName();
    }

    /**
     * Log the placement of a block. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @param player player to commit action
     * @param block  the block that is placed
     */
    public static void logPlacement(OfflinePlayer player, Block block) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().logPlacement(player, block);
        }
    }

    /**
     * Log the removal of a block. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @param player player to commit actionremvedplaced
     */
    public static void logRemoval(OfflinePlayer player, Block block) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().logRemoval(player, block);
        }
    }

    /**
     * Log a player interaction. <br />
     * NOTE: using a default log assumes that this library is shaded
     *
     * @param player   player to commit action
     * @param location the location that is interacted with
     */
    public static void logInteraction(OfflinePlayer player, Location location) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().logInteraction(player, location);
        }
    }
}
