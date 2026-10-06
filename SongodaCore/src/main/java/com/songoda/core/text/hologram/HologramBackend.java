package com.songoda.core.text.hologram;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Keeps optional hologram backends isolated from the core manager. In
 * particular, loading SongodaCore without ProtocolLib must not resolve any
 * ProtocolLib classes.
 */
abstract class HologramBackend {

    abstract boolean isSupported();

    abstract void init();

    abstract void render(Hologram hologram, boolean force);

    abstract void remove(Hologram hologram);

    abstract void clear();

    abstract void onPlayerJoin(Player player);

    abstract void onPlayerQuit(UUID playerId);

    abstract void onPlayerChangedWorld(Player player);

    abstract void onServerChunkLoad(Chunk chunk);

    abstract void onServerChunkUnload(Chunk chunk);
}
