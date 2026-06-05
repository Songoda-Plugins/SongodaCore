package com.craftaro.core.compatibility.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Run a task on Folia Scheduler or fallback for Bukkit Scheduler
 * Delay is in ticks
 */
public class SchedulerUtils {

    // Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);

    //TODO: Add methods for simple Runnable

    private static boolean isFolia;

    public static boolean isFolia() {
        return isFolia;
    }

    public static boolean isOwnedByCurrentRegion(Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    public static boolean isOwnedByCurrentRegion(Block block) {
        return Bukkit.isOwnedByCurrentRegion(block);
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    //Entity
    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        if (isFolia) {
            return new SchedulerTask(entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null,  correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        if (isFolia) {
            return new SchedulerTask(entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), null, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            return new SchedulerTask(entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), null, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Location
    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> {
                runnable.run();
            }));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run()));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Default
    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run()));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run()));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run()));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run()));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay), TimeUnit.MILLISECONDS));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay), TimeUnit.MILLISECONDS));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia) {
            SchedulerTask task =  new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        //return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, correctDelay(delay), period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia) {
            SchedulerTask task = new SchedulerTask(plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period), TimeUnit.MILLISECONDS));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia) {
            return new SchedulerTask(plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), correctDelay(delay), correctDelay(period), TimeUnit.MILLISECONDS));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    public static void cancelTask(SchedulerTask task) {
        if (task == null) return;
        if (isFolia) {
            task.getAsFoliaTask().cancel();
        } else {
            Bukkit.getScheduler().cancelTask(task.getAsBukkitTaskId());
        }
    }

    public static void cancelAllTasks(Plugin plugin) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    public static boolean isCurrentlyRunning(SchedulerTask task) {
        if (isFolia) {
            return task.getAsFoliaTask().getExecutionState() == ScheduledTask.ExecutionState.RUNNING;
        } else {
            return Bukkit.getScheduler().isCurrentlyRunning(task.getAsBukkitTaskId());
        }
    }

    public static boolean isQueued(SchedulerTask task) {
        if (isFolia) {
            return task.getAsFoliaTask().getExecutionState() == ScheduledTask.ExecutionState.RUNNING;
        } else {
            return Bukkit.getScheduler().isQueued(task.getAsBukkitTaskId());
        }
    }

    private static long correctDelay(long delay) {
        if (isFolia && delay <= 0) {
            return 1;
        }
        return delay;
    }
}
