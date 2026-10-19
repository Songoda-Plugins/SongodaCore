package com.songoda.core.compatibility.folia;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Processes entities periodically on their owner. Paper uses one synchronous world scan;
 * Folia uses native entity timers, discovered through entity load and spawn lifecycle events.
 * The only shared collection holds live scheduler handles and is drained on retirement or close.
 * A caller-owned PDC key deduplicates native timers; it never contains plugin gameplay data.
 *
 * @param <T> entity type processed by the task
 */
public final class EntityTaskScheduler<T extends Entity> implements Listener, AutoCloseable {

    private final Plugin plugin;
    private final Class<T> entityType;
    private final NamespacedKey taskMarker;
    private final BooleanSupplier enabled;
    private final Predicate<World> worldFilter;
    private final LongSupplier intervalTicks;
    private final LongSupplier initialDelayTicks;
    private final Predicate<T> entityFilter;
    private final Consumer<T> processor;
    private final Consumer<List<T>> batchProcessor;
    private final String generation = UUID.randomUUID().toString();
    private final Set<NativeTask> nativeTasks = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;
    private BukkitTask paperTask;

    private EntityTaskScheduler(Plugin plugin,
            Class<T> entityType,
            NamespacedKey taskMarker,
            BooleanSupplier enabled,
            Predicate<World> worldFilter,
            LongSupplier intervalTicks,
            LongSupplier initialDelayTicks,
            Predicate<T> entityFilter,
            Consumer<T> processor,
            Consumer<List<T>> batchProcessor) {
        this.plugin = plugin;
        this.entityType = entityType;
        this.taskMarker = taskMarker;
        this.enabled = enabled;
        this.worldFilter = worldFilter;
        this.intervalTicks = intervalTicks;
        this.initialDelayTicks = initialDelayTicks;
        this.entityFilter = entityFilter;
        this.processor = processor;
        this.batchProcessor = batchProcessor;
    }

    /**
     * Starts a processor. Predicates and processing callbacks run on the entity owner; world
     * filters also run before Paper scans a world. Keep filters cheap and free of side effects.
     * Interval changes take effect at the next existing timer callback. Entity filters should
     * describe stable exclusions; enabled/world filters are rechecked on every processing pass.
     * Close the returned scheduler when the owning component is destroyed.
     *
     * @param plugin plugin that owns the tasks
     * @param entityType class of entities to process
     * @param taskMarker PDC key reserved for this scheduler's transient timer identity
     * @param enabled whether processing is currently enabled
     * @param worldFilter worlds eligible for processing
     * @param intervalTicks interval in ticks, clamped to at least one
     * @param entityFilter stable entity exclusions, evaluated before Folia timers are created
     * @param processor action to execute on the owning thread
     * @param <T> entity type processed by the task
     * @return running scheduler
     */
    public static <T extends Entity> @NotNull EntityTaskScheduler<T> start(@NotNull Plugin plugin,
            @NotNull Class<T> entityType,
            @NotNull NamespacedKey taskMarker,
            @NotNull BooleanSupplier enabled,
            @NotNull Predicate<World> worldFilter,
            @NotNull LongSupplier intervalTicks,
            @NotNull Predicate<T> entityFilter,
            @NotNull Consumer<T> processor) {
        return new EntityTaskScheduler<>(plugin, entityType, taskMarker, enabled, worldFilter, intervalTicks, intervalTicks, entityFilter, processor, null).initialize();
    }

    /**
     * Starts owner-local batches. Paper delivers one filtered batch per world and interval;
     * Folia delivers singleton batches on each entity's native scheduler. Batch-local scratch
     * state can be discarded after each call, without persistent entity tracking.
     *
     * @param plugin plugin owning the tasks
     * @param entityType entity class to discover
     * @param taskMarker transient native timer identity key
     * @param enabled whether processing is enabled
     * @param worldFilter eligible worlds
     * @param intervalTicks interval in ticks
     * @param entityFilter stable exclusions
     * @param processor owner-local batch action
     * @param <T> entity type
     * @return running scheduler to close on component destruction
     */
    public static <T extends Entity> @NotNull EntityTaskScheduler<T> startBatches(@NotNull Plugin plugin,
            @NotNull Class<T> entityType,
            @NotNull NamespacedKey taskMarker,
            @NotNull BooleanSupplier enabled,
            @NotNull Predicate<World> worldFilter,
            @NotNull LongSupplier intervalTicks,
            @NotNull Predicate<T> entityFilter,
            @NotNull Consumer<List<T>> processor) {
        return new EntityTaskScheduler<>(plugin, entityType, taskMarker, enabled, worldFilter, intervalTicks, intervalTicks, entityFilter, entity -> { }, processor).initialize();
    }

    /**
     * Starts owner-local batches with a caller-supplied initial delay. The initial delay is
     * evaluated for each discovered entity, while subsequent runs use the configured interval.
     *
     * @param plugin plugin owning the tasks
     * @param entityType entity class to discover
     * @param taskMarker transient native timer identity key
     * @param enabled whether processing is enabled
     * @param worldFilter eligible worlds
     * @param intervalTicks repeating interval in ticks
     * @param initialDelayTicks initial delay in ticks for each entity timer
     * @param entityFilter stable entity exclusions
     * @param processor owner-local batch action
     * @param <T> entity type
     * @return running scheduler to close on component destruction
     */
    public static <T extends Entity> @NotNull EntityTaskScheduler<T> startBatches(@NotNull Plugin plugin,
            @NotNull Class<T> entityType,
            @NotNull NamespacedKey taskMarker,
            @NotNull BooleanSupplier enabled,
            @NotNull Predicate<World> worldFilter,
            @NotNull LongSupplier intervalTicks,
            @NotNull LongSupplier initialDelayTicks,
            @NotNull Predicate<T> entityFilter,
            @NotNull Consumer<List<T>> processor) {
        return new EntityTaskScheduler<>(plugin, entityType, taskMarker, enabled, worldFilter, intervalTicks, initialDelayTicks, entityFilter, entity -> { }, processor).initialize();
    }

    private EntityTaskScheduler<T> initialize() {
        try {
            if (SchedulerUtils.isFolia()) {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                bootstrapFoliaEntities();
            } else {
                schedulePaper();
            }
            return this;
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    private long interval() {
        return Math.max(1L, intervalTicks.getAsLong());
    }

    private synchronized void schedulePaper() {
        if (!running) {
            return;
        }
        long period = interval();
        paperTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                scanPaper();
            } finally {
                if (running && interval() != period) {
                    paperTask.cancel();
                    schedulePaper();
                }
            }
        }, period, period);
    }

    private void scanPaper() {
        if (!running || !plugin.isEnabled() || !enabled.getAsBoolean()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            if (!worldFilter.test(world)) {
                continue;
            }
            List<T> batch = batchProcessor == null ? null : new ArrayList<>();
            for (T entity : world.getEntitiesByClass(entityType)) {
                if (batch == null) {
                    processSafely(entity);
                } else if (SchedulerUtils.isOwnedByCurrentRegion(entity) && entity.isValid() && entityFilter.test(entity)) {
                    batch.add(entity);
                }
            }
            if (batch != null && !batch.isEmpty() && running && enabled.getAsBoolean()) {
                processBatchSafely(batch);
            }
        }
    }

    private void processSafely(T entity) {
        try {
            if (running && plugin.isEnabled() && enabled.getAsBoolean()
                    && SchedulerUtils.isOwnedByCurrentRegion(entity) && entity.isValid()
                    && worldFilter.test(entity.getWorld()) && entityFilter.test(entity)) {
                if (batchProcessor == null) {
                    processor.accept(entity);
                } else {
                    processBatchSafely(List.of(entity));
                }
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process an entity in a periodic task", exception);
        }
    }

    private void processBatchSafely(List<T> entities) {
        try {
            batchProcessor.accept(entities);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process an entity batch in a periodic task", exception);
        }
    }

    private void bootstrapFoliaEntities() {
        NativeTask bootstrap = track();
        bootstrap.install(SchedulerUtils.runTask(plugin, () -> {
            bootstrap.finish(false);
            if (!running) {
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                // CraftWorld snapshots coordinates from the concurrent fullChunks index. Reading
                // chunk contents is deferred to the owner, and never loads an absent chunk.
                for (Chunk chunk : world.getLoadedChunks()) {
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    Location location = new Location(world, chunkX * 16.0 + 8.0, 0.0, chunkZ * 16.0 + 8.0);
                    NativeTask scan = track();
                    scan.install(SchedulerUtils.runLocationTask(plugin, location, () -> {
                        scan.finish(false);
                        if (!running || !SchedulerUtils.isOwnedByCurrentRegion(location) || !world.isChunkLoaded(chunkX, chunkZ)) {
                            return;
                        }
                        for (Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
                            discover(entity, false);
                        }
                    }));
                }
            }
        }));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAdded(EntityAddToWorldEvent event) {
        discover(event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoaded(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            // A persisted marker may belong to a retired timer from an earlier load.
            discover(entity, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnloaded(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entityType.isInstance(entity) && SchedulerUtils.isOwnedByCurrentRegion(entity)) {
                String marker = entity.getPersistentDataContainer().get(taskMarker, PersistentDataType.STRING);
                if (marker != null && marker.startsWith(generation + ":")) {
                    entity.getPersistentDataContainer().remove(taskMarker);
                }
            }
        }
    }

    private void discover(Entity entity, boolean replaceMarker) {
        if (!running || !entityType.isInstance(entity)) {
            return;
        }
        T candidate = entityType.cast(entity);
        if (SchedulerUtils.isOwnedByCurrentRegion(candidate)) {
            attach(candidate, replaceMarker);
            return;
        }
        NativeTask discovery = track();
        discovery.install(SchedulerUtils.runEntityTask(plugin, candidate, () -> {
            discovery.finish(false);
            attach(candidate, replaceMarker);
        }, () -> discovery.finish(false)));
    }

    private void attach(T entity, boolean replaceMarker) {
        if (!running || !SchedulerUtils.isOwnedByCurrentRegion(entity) || !entity.isValid() || !entityFilter.test(entity)) {
            return;
        }
        String previousMarker = entity.getPersistentDataContainer().get(taskMarker, PersistentDataType.STRING);
        if (!replaceMarker && previousMarker != null && previousMarker.startsWith(generation + ":")) {
            return;
        }
        String marker = generation + ":" + UUID.randomUUID();
        entity.getPersistentDataContainer().set(taskMarker, PersistentDataType.STRING, marker);
        long period = interval();
        long delay = Math.max(1L, initialDelayTicks.getAsLong());
        NativeTask timer = track();
        try {
            timer.install(SchedulerUtils.runEntityTaskTimer(plugin, entity, () -> {
                if (!running || !plugin.isEnabled()) {
                    timer.finish(true);
                    return;
                }
                if (!SchedulerUtils.isOwnedByCurrentRegion(entity)
                        || !marker.equals(entity.getPersistentDataContainer().get(taskMarker, PersistentDataType.STRING))) {
                    timer.finish(true);
                    return;
                }
                if (!entity.isValid()) {
                    entity.getPersistentDataContainer().remove(taskMarker);
                    timer.finish(true);
                    return;
                }
                processSafely(entity);
                if (running && period != interval()) {
                    timer.finish(true);
                    attach(entity, true);
                }
            }, () -> timer.finish(false), delay, period));
        } catch (RuntimeException | Error exception) {
            timer.finish(true);
            if (SchedulerUtils.isOwnedByCurrentRegion(entity)
                    && marker.equals(entity.getPersistentDataContainer().get(taskMarker, PersistentDataType.STRING))) {
                entity.getPersistentDataContainer().remove(taskMarker);
            }
            throw exception;
        }
    }

    private NativeTask track() {
        NativeTask task = new NativeTask();
        nativeTasks.add(task);
        if (!running) {
            task.finish(true);
        }
        return task;
    }

    /** Stops processing and cancels all live scheduler handles. Safe to call repeatedly. */
    @Override
    public synchronized void close() {
        running = false;
        if (paperTask != null) {
            paperTask.cancel();
            paperTask = null;
        }
        HandlerList.unregisterAll(this);
        for (NativeTask task : nativeTasks) {
            task.finish(true);
        }
        nativeTasks.clear();
    }

    private final class NativeTask {

        private SchedulerTask task;
        private boolean finished;

        private synchronized void install(SchedulerTask scheduledTask) {
            task = scheduledTask;
            if (scheduledTask == null) {
                finish(false);
            } else if (finished) {
                SchedulerUtils.cancelTask(scheduledTask);
            }
        }

        private synchronized void finish(boolean cancel) {
            finished = true;
            nativeTasks.remove(this);
            if (cancel && task != null) {
                SchedulerUtils.cancelTask(task);
            }
        }
    }
}
