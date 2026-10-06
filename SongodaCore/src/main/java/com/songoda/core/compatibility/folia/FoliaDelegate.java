package com.songoda.core.compatibility.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface FoliaDelegate {
    Object runEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired);

    Object runEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delay);

    Object runEntityTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period);

    Object runLocation(Plugin plugin, Location location, Runnable runnable);

    Object runLocationLater(Plugin plugin, Location location, Runnable runnable, long delay);

    Object runLocationTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period);

    Object runGlobal(Plugin plugin, Runnable runnable);

    Object runGlobalLater(Plugin plugin, Runnable runnable, long delay);

    Object runGlobalTimer(Plugin plugin, Runnable runnable, long delay, long period);

    Object runAsync(Plugin plugin, Runnable runnable);

    Object runAsyncLater(Plugin plugin, Runnable runnable, long delay);

    Object runAsyncTimer(Plugin plugin, Runnable runnable, long delay, long period);

    void cancelTask(Object task);

    void cancelAllTasks(Plugin plugin);

    boolean isCurrentlyRunning(Object task);

    boolean isQueued(Object task);
}
