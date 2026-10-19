package com.songoda.core.compatibility.folia;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.compatibility.KnownServerVersions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Run a task on Folia Scheduler or fallback for Bukkit Scheduler
 * Delay is in ticks
 */
public class SchedulerUtils {

    private static boolean isFolia;
    private static FoliaDelegate foliaDelegate;
    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            foliaDelegate = (FoliaDelegate) Class.forName("com.songoda.core.compatibility.folia.FoliaDelegateImpl").getConstructor().newInstance();
        } catch (Throwable e) {
            isFolia = false;
            foliaDelegate = null;
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    /**
     * Checks whether the current thread owns the location's region, or is the primary
     * thread on servers without Folia ownership support. Null and worldless locations,
     * and ownership checks that throw {@link IllegalStateException}, return false.
     *
     * @param location the location to check, or null when its owner is unknown
     * @return whether region-owned state at this location can be accessed
     */
    public static boolean isOwnedByCurrentRegion(@Nullable Location location) {
        try {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            if (!isFolia || !ServerVersion.isAtLeastVersion(KnownServerVersions.V1_19_4)) {
                return Bukkit.isPrimaryThread();
            }
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    /**
     * Checks Folia block ownership, or primary-thread access on other servers.
     * Null blocks and unavailable ownership return false without reading block state.
     *
     * @param block the block to check, or null when its owner is unknown
     * @return whether the current thread owns the block
     */
    public static boolean isOwnedByCurrentRegion(@Nullable Block block) {
        try {
            if (block == null) {
                return false;
            }
            if (!isFolia || !ServerVersion.isAtLeastVersion(KnownServerVersions.V1_19_4)) {
                return Bukkit.isPrimaryThread();
            }
            return Bukkit.isOwnedByCurrentRegion(block);
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    /**
     * Checks ownership of both targets before an operation involving an entity and block.
     *
     * @param entity the entity to check, or null when its owner is unknown
     * @param block the block to check, or null when its owner is unknown
     * @return whether the current thread owns both targets
     */
    public static boolean isOwnedByCurrentRegion(@Nullable Entity entity, @Nullable Block block) {
        return isOwnedByCurrentRegion(entity) && isOwnedByCurrentRegion(block);
    }

    /**
     * Checks Folia entity ownership, or primary-thread access on other servers.
     * Null entities and unavailable ownership return false without reading entity data.
     *
     * @param entity the entity to check, or null when its owner is unknown
     * @return whether the current thread owns the entity
     */
    public static boolean isOwnedByCurrentRegion(@Nullable Entity entity) {
        try {
            if (entity == null) {
                return false;
            }
            if (!isFolia || !ServerVersion.isAtLeastVersion(KnownServerVersions.V1_19_4)) {
                return Bukkit.isPrimaryThread();
            }
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    /**
     * Resolves an inventory's entity holder or block-state location before falling back
     * to its location. Does not read inventory contents. Null inventories, unknown or
     * worldless locations, and owners unavailable through {@link IllegalStateException}
     * return false. Other servers still require the primary thread.
     *
     * @param inventory the inventory whose owner should be checked
     * @return whether the current thread owns the resolved inventory owner
     */
    public static boolean isOwnedByCurrentRegion(@Nullable Inventory inventory) {
        try {
            if (inventory == null) {
                return false;
            }
            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof Entity entity) {
                return isOwnedByCurrentRegion(entity);
            }
            if (holder instanceof BlockState blockState) {
                return isOwnedByCurrentRegion(blockState.getLocation());
            }
            return isOwnedByCurrentRegion(inventory.getLocation());
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public static void requireOwnedByCurrentRegion(@Nullable Location location, @NotNull String action) {
        if (location == null || location.getWorld() == null || !isOwnedByCurrentRegion(location)) {
            throw new IllegalStateException(action + " must run on the primary thread or owning Folia region.");
        }
    }

    public static void requireOwnedByCurrentRegion(@NotNull Block block, @NotNull String action) {
        if (!isOwnedByCurrentRegion(block)) {
            throw new IllegalStateException(action + " must run on the primary thread or owning Folia region.");
        }
    }

    public static void requireOwnedByCurrentRegion(@NotNull Entity entity, @NotNull String action) {
        if (!isOwnedByCurrentRegion(entity)) {
            throw new IllegalStateException(action + " must run on the primary thread or owning Folia region.");
        }
    }

    public static void requireFoliaOwnedByCurrentRegion(@NotNull Entity entity, @NotNull String action) {
        if (isFolia() && !isOwnedByCurrentRegion(entity)) {
            throw new IllegalStateException(action + " must run on the entity's owning Folia region.");
        }
    }

    //Entity
    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntity(plugin, entity, runnable, null));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntity(plugin, entity, runnable, null));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static @Nullable SchedulerTask runEntityTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired) {
        if (isFolia && foliaDelegate != null) {
            Object task = foliaDelegate.runEntity(plugin, entity, runnable, retired);
            if (task == null) {
                runTask(plugin, retired);
                return null;
            }
            return new SchedulerTask(task);
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntityLater(plugin, entity, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntityLater(plugin, entity, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }


    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runEntityTimer(plugin, entity, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runEntityTimer(plugin, entity, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    /**
     * Runs a repeating entity task with retirement cleanup on Folia, or a synchronous
     * Bukkit timer on other servers. Retirement cleanup must not access entity or world
     * state. If the entity has already retired, cleanup runs and no task is returned.
     *
     * @param plugin owner of the timer
     * @param entity entity whose scheduler should execute the task
     * @param runnable periodic action on the entity owner
     * @param retired cleanup for an entity that retires before further execution
     * @param delay initial delay in ticks
     * @param period interval in ticks
     * @return timer handle, or null if the entity has already retired
     */
    public static @Nullable SchedulerTask runEntityTaskTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @NotNull Runnable retired, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            Object task = foliaDelegate.runEntityTimer(plugin, entity, runnable, retired, correctDelay(delay), correctDelay(period));
            if (task == null) {
                retired.run();
                return null;
            }
            return new SchedulerTask(task);
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Location
    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocation(plugin, location, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocation(plugin, location, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }


    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocationLater(plugin, location, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocationLater(plugin, location, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runLocationTimer(plugin, location, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runLocationTaskTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runLocationTimer(plugin, location, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    //Default
    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobal(plugin, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobal(plugin, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsync(plugin, runnable));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsync(plugin, runnable));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsyncLater(plugin, runnable, asyncDelayMillis(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsyncLater(plugin, runnable, asyncDelayMillis(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalLater(plugin, runnable, correctDelay(delay)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay));
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask scheduleSyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runGlobalTimer(plugin, runnable, correctDelay(delay), correctDelay(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull SchedulerRunnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            SchedulerTask task = new SchedulerTask(foliaDelegate.runAsyncTimer(plugin, runnable, asyncDelayMillis(delay), asyncDelayMillis(period)));
            runnable.setTask(task);
            return task;
        }
        SchedulerTask task = new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
        runnable.setTask(task);
        return task;
    }

    public static SchedulerTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        if (isFolia && foliaDelegate != null) {
            return new SchedulerTask(foliaDelegate.runAsyncTimer(plugin, runnable, asyncDelayMillis(delay), asyncDelayMillis(period)));
        }
        return new SchedulerTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    public static void cancelTask(SchedulerTask task) {
        if (task == null) return;
        if (isFolia) {
            foliaDelegate.cancelTask(task.getTask());
        } else {
            Bukkit.getScheduler().cancelTask(task.getAsBukkitTaskId());
        }
    }

    public static void cancelAllTasks(Plugin plugin) {
        if (isFolia) {
            foliaDelegate.cancelAllTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    public static boolean isCurrentlyRunning(SchedulerTask task) {
        if (isFolia) {
            return foliaDelegate.isCurrentlyRunning(task.getTask());
        } else {
            return Bukkit.getScheduler().isCurrentlyRunning(task.getAsBukkitTaskId());
        }
    }

    public static boolean isQueued(SchedulerTask task) {
        if (isFolia) {
            return foliaDelegate.isQueued(task.getTask());
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

    private static long asyncDelayMillis(long delayTicks) {
        long safeTicks = Math.max(1L, delayTicks);
        return safeTicks > Long.MAX_VALUE / 50L ? Long.MAX_VALUE : safeTicks * 50L;
    }
}
