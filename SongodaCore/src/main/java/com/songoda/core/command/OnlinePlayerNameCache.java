package com.songoda.core.command;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.vinject.annotation.RegisterListener;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RegisterListener
public final class OnlinePlayerNameCache implements Listener {

    private static final Map<UUID, String> NAMES_BY_PLAYER = new ConcurrentHashMap<>();

    @PostConstruct
    public void cacheOnlinePlayers() {
        SchedulerUtils.runTask(SongodaPlugin.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cache(player);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        cache(event.getPlayer());
    }

    private void cache(Player player) {
        SchedulerUtils.runEntityTask(SongodaPlugin.getInstance(), player, () -> {
            if (player.isOnline()) {
                cacheName(player.getUniqueId(), player.getName());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeName(event.getPlayer().getUniqueId());
    }

    static List<String> findNamesStartingWith(String partial) {
        String prefix = partial.toLowerCase(Locale.ROOT);
        return NAMES_BY_PLAYER.values().stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
    }

    static void cacheName(UUID playerId, String name) {
        NAMES_BY_PLAYER.put(playerId, name);
    }

    static void removeName(UUID playerId) {
        NAMES_BY_PLAYER.remove(playerId);
    }
}
