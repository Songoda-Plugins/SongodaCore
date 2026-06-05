package com.songoda.core.hooks.protection;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.OutdatedHookInterface;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public abstract class Protection implements OutdatedHookInterface {
    protected final Plugin plugin;

    public Protection(Plugin plugin) {
        this.plugin = plugin;
    }

    public abstract boolean canPlace(Player player, Location location);

    public abstract boolean canBreak(Player player, Location location);

    public abstract boolean canInteract(Player player, Location location);
}
