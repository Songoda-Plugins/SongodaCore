package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.protection.Protection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class ProtectionManager {
    private static final HookManager<Protection> manager = new HookManager(Protection.class);

    /**
     * Load all supported protection plugins.<br/>
     * Note: This method should be called in your plugin's onEnable() section
     *
     * @param plugin plugin that will be using the protection hooks.
     */
    public static void load(Plugin plugin) {
        manager.load(plugin);
    }

    public static HookManager getManager() {
        return manager;
    }

    public static boolean canPlace(Player player, Location location) {
        return manager.getRegisteredHooks().stream().allMatch(protection -> protection.canPlace(player, location));
    }

    public static boolean canBreak(Player player, Location location) {
        return manager.getRegisteredHooks().stream().allMatch(protection -> protection.canBreak(player, location));
    }

    public static boolean canInteract(Player player, Location location) {
        return manager.getRegisteredHooks().stream().allMatch(protection -> protection.canInteract(player, location));
    }
}
