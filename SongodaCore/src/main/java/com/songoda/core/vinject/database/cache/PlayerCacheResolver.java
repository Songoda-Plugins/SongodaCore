package com.songoda.core.vinject.database.cache;

import net.vortexdevelopment.vinject.annotation.Inject;
import net.vortexdevelopment.vinject.database.cache.CacheCoordinator;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RegisterListener
public class PlayerCacheResolver implements Listener {

    @Inject
    private CacheCoordinator cacheCoordinator;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        cacheCoordinator.load(DefaultCacheKeys.PLAYER_UUID, playerUUID);
        cacheCoordinator.load(DefaultCacheKeys.PLAYER_NAME, playerName);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        cacheCoordinator.unload(DefaultCacheKeys.PLAYER_UUID, playerUUID);
        cacheCoordinator.unload(DefaultCacheKeys.PLAYER_NAME, playerName);
    }
}
