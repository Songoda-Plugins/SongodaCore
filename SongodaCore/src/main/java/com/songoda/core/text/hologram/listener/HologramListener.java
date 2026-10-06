package com.songoda.core.text.hologram.listener;

import com.songoda.core.text.hologram.HologramManager;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

@RegisterListener
public class HologramListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ArmorStand armorStand) {

                // First, clear any out of session holograms in this chunk
                String sessionId = armorStand.getPersistentDataContainer().get(HologramManager.getSessionIdKey(), PersistentDataType.STRING);
                if (sessionId != null && sessionId.equals(HologramManager.getSessionId())) {
                    armorStand.remove();
                }
            }
        }

        // Now load holograms for this chunk
        HologramManager.loadHologramsInChunk(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        HologramManager.unloadHologramsInChunk(event.getChunk());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HologramManager.onPlayerJoin(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        HologramManager.onPlayerQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        HologramManager.onPlayerChangedWorld(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HologramManager.onPlayerChangedWorld(event.getPlayer());
    }
}
