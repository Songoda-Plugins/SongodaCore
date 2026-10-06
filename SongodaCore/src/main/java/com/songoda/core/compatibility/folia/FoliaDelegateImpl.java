package com.songoda.core.compatibility.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaDelegateImpl implements FoliaDelegate {

    @Override
    public Object runEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        return entity.getScheduler().run(plugin, callback(runnable), retired == null ? () -> { } : retired);
    }

    @Override
    public Object runEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        return entity.getScheduler().runDelayed(plugin, callback(runnable), () -> { }, delay);
    }

    @Override
    public Object runEntityTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        return entity.getScheduler().runAtFixedRate(plugin, callback(runnable), () -> { }, delay, period);
    }

    @Override
    public Object runLocation(Plugin plugin, Location location, Runnable runnable) {
        return plugin.getServer().getRegionScheduler().run(plugin, location, callback(runnable));
    }

    @Override
    public Object runLocationLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        return plugin.getServer().getRegionScheduler().runDelayed(plugin, location, callback(runnable), delay);
    }

    @Override
    public Object runLocationTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        return plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, callback(runnable), delay, period);
    }

    @Override
    public Object runGlobal(Plugin plugin, Runnable runnable) {
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, callback(runnable));
    }

    @Override
    public Object runGlobalLater(Plugin plugin, Runnable runnable, long delay) {
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, callback(runnable), delay);
    }

    @Override
    public Object runGlobalTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, callback(runnable), delay, period);
    }

    @Override
    public Object runAsync(Plugin plugin, Runnable runnable) {
        return plugin.getServer().getAsyncScheduler().runNow(plugin, callback(runnable));
    }

    @Override
    public Object runAsyncLater(Plugin plugin, Runnable runnable, long delay) {
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, callback(runnable), delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object runAsyncTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, callback(runnable), delay, period,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancelTask(Object task) {
        ((ScheduledTask) task).cancel();
    }

    @Override
    public void cancelAllTasks(Plugin plugin) {
        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
        plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
    }

    @Override
    public boolean isCurrentlyRunning(Object task) {
        return ((ScheduledTask) task).getExecutionState() == ScheduledTask.ExecutionState.RUNNING;
    }

    @Override
    public boolean isQueued(Object task) {
        return ((ScheduledTask) task).getExecutionState() == ScheduledTask.ExecutionState.IDLE;
    }

    private static Consumer<ScheduledTask> callback(Runnable runnable) {
        return task -> runnable.run();
    }
}
