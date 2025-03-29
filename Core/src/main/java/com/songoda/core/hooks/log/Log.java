package com.songoda.core.hooks.log;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.OutdatedHookInterface;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public abstract class Log implements OutdatedHookInterface {
    public abstract void logPlacement(OfflinePlayer player, Block block);

    public abstract void logRemoval(OfflinePlayer player, Block block);

    public abstract void logInteraction(OfflinePlayer player, Location location);
}
