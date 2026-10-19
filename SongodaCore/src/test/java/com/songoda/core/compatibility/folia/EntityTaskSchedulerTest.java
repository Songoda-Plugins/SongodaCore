package com.songoda.core.compatibility.folia;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntityTaskSchedulerTest {

    private final List<World> worlds = new ArrayList<>();
    private final List<PaperJob> paperJobs = new ArrayList<>();
    private final List<NativeJob> nativeJobs = new ArrayList<>();
    private final List<LivingEntity> processed = new ArrayList<>();
    private final List<EntityTaskScheduler<?>> schedulers = new ArrayList<>();
    private final NamespacedKey marker = NamespacedKey.fromString("test:entity_task");
    private Field serverField;
    private Field foliaField;
    private Field delegateField;
    private Object previousServer;
    private boolean previousFolia;
    private Object previousDelegate;
    private Server server;
    private Plugin plugin;
    private boolean enabled = true;
    private boolean owner = true;
    private long interval = 3L;
    private int scans;
    private int worldEnumerations;

    @Before
    public void installServer() throws ReflectiveOperationException {
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        BukkitScheduler bukkitScheduler = proxy(BukkitScheduler.class, (instance, method, args) -> {
            if (method.getName().equals("runTaskTimer")) {
                PaperJob job = new PaperJob((Runnable) args[1], (Long) args[3]);
                paperJobs.add(job);
                return job.task;
            }
            throw new AssertionError("Unexpected scheduler access: " + method.getName());
        });
        PluginManager pluginManager = proxy(PluginManager.class, (instance, method, args) -> null);
        GlobalRegionScheduler globalScheduler = proxy(GlobalRegionScheduler.class, (instance, method, args) -> {
            if (method.getName().equals("run")) {
                return newNativeJob(args[1], null).task;
            }
            throw new AssertionError("Unexpected global scheduler access: " + method.getName());
        });
        server = proxy(Server.class, (instance, method, args) -> switch (method.getName()) {
            case "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "getVersion" -> "Paper (MC: 1.21.4)";
            case "getScheduler" -> bukkitScheduler;
            case "getGlobalRegionScheduler" -> globalScheduler;
            case "getPluginManager" -> pluginManager;
            case "getWorlds" -> {
                worldEnumerations++;
                yield worlds;
            }
            case "isPrimaryThread", "isOwnedByCurrentRegion" -> owner;
            default -> throw new AssertionError("Unexpected server access: " + method.getName());
        });
        serverField.set(null, server);
        foliaField = SchedulerUtils.class.getDeclaredField("isFolia");
        foliaField.setAccessible(true);
        previousFolia = foliaField.getBoolean(null);
        foliaField.setBoolean(null, false);
        delegateField = SchedulerUtils.class.getDeclaredField("foliaDelegate");
        delegateField.setAccessible(true);
        previousDelegate = delegateField.get(null);
        delegateField.set(null, new FoliaDelegateImpl());
        plugin = proxy(Plugin.class, (instance, method, args) -> switch (method.getName()) {
            case "getServer" -> server;
            case "isEnabled" -> true;
            case "getLogger" -> Logger.getLogger("entity-task-test");
            case "getName" -> "EntityTaskTest";
            default -> throw new AssertionError("Unexpected plugin access: " + method.getName());
        });
    }

    @After
    public void restoreServer() throws IllegalAccessException {
        for (EntityTaskScheduler<?> scheduler : schedulers) {
            scheduler.close();
        }
        delegateField.set(null, previousDelegate);
        foliaField.setBoolean(null, previousFolia);
        serverField.set(null, previousServer);
    }

    @Test
    public void paperUsesOneTimerAndFiltersWorldsBeforeEnumeratingEntities() {
        World included = world("included", new ArrayList<>());
        World excluded = world("excluded", new ArrayList<>());
        LivingEntity entity = entity(included);
        worlds.add(included);
        worlds.add(excluded);
        start(world -> world == included, candidate -> true, processed::add);

        assertEquals(1, paperJobs.size());
        assertEquals(3L, paperJobs.get(0).period);
        paperJobs.get(0).action.run();

        assertEquals(1, scans);
        assertEquals(List.of(entity), processed);
    }

    @Test
    public void commonTickerCanBeInspectedWithoutNativeFoliaClasses() throws Exception {
        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.papermc.paper.threadedregions")) {
                    throw new ClassNotFoundException(name);
                }
                if (!name.startsWith(EntityTaskScheduler.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (var input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = input.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        };

        Class<?> ticker = Class.forName(EntityTaskScheduler.class.getName(), false, isolated);

        assertTrue(ticker.getDeclaredMethods().length > 0);
        for (Class<?> nested : ticker.getDeclaredClasses()) {
            nested.getDeclaredMethods();
        }
    }

    @Test
    public void disabledProcessorDoesNotEnumerateWorldsOrEntities() {
        worlds.add(world("world", new ArrayList<>()));
        enabled = false;
        start(world -> true, entity -> true, processed::add);

        paperJobs.get(0).action.run();

        assertEquals(0, worldEnumerations);
        assertEquals(0, scans);
    }

    @Test
    public void paperDeliversOneOwnerLocalBatchPerWorld() {
        World world = world("world", new ArrayList<>());
        LivingEntity first = entity(world);
        LivingEntity second = entity(world);
        worlds.add(world);
        List<List<LivingEntity>> batches = new ArrayList<>();
        EntityTaskScheduler<LivingEntity> scheduler = EntityTaskScheduler.startBatches(plugin, LivingEntity.class, marker,
                () -> enabled, candidate -> true, () -> interval, candidate -> true, batch -> batches.add(List.copyOf(batch)));
        schedulers.add(scheduler);

        paperJobs.get(0).action.run();

        assertEquals(List.of(List.of(first, second)), batches);
        assertEquals(1, scans);
        assertEquals(1, paperJobs.size());
    }

    @Test
    public void entityFilterRunsBeforeProcessing() {
        World world = world("world", new ArrayList<>());
        LivingEntity included = entity(world);
        entity(world);
        worlds.add(world);
        start(candidate -> true, candidate -> candidate == included, processed::add);

        paperJobs.get(0).action.run();

        assertEquals(List.of(included), processed);
    }

    @Test
    public void paperIntervalChangeReplacesTheSingleTimer() {
        start(world -> true, entity -> true, processed::add);
        interval = 9L;

        paperJobs.get(0).action.run();

        assertEquals(1, paperJobs.get(0).cancellations);
        assertEquals(2, paperJobs.size());
        assertEquals(9L, paperJobs.get(1).period);
        assertEquals(0, paperJobs.get(1).cancellations);
    }

    @Test
    public void closeIsIdempotentAndPreventsFurtherScans() {
        EntityTaskScheduler<LivingEntity> scheduler = start(world -> true, entity -> true, processed::add);

        scheduler.close();
        scheduler.close();
        paperJobs.get(0).action.run();

        assertEquals(1, paperJobs.get(0).cancellations);
        assertEquals(0, worldEnumerations);
    }

    @Test
    public void foliaDeduplicatesNotificationsAndCloseCancelsNativeTimers() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        World world = world("world", new ArrayList<>());
        LivingEntity entity = entity(world);
        EntityTaskScheduler<LivingEntity> scheduler = start(candidate -> true, candidate -> true, processed::add);

        scheduler.onEntityAdded(new EntityAddToWorldEvent(entity, world));
        scheduler.onEntityAdded(new EntityAddToWorldEvent(entity, world));
        assertEquals(2, nativeJobs.size());
        nativeJobs.get(1).run();
        assertEquals(List.of(entity), processed);

        scheduler.close();
        assertTrue(nativeJobs.get(0).cancelled);
        assertTrue(nativeJobs.get(1).cancelled);
    }

    @Test
    public void foliaDoesNotInspectForeignEntityUntilItsScheduledCallback() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        World world = world("world", new ArrayList<>());
        LivingEntity entity = entity(world);
        EntityTaskScheduler<LivingEntity> scheduler = start(candidate -> true, candidate -> true, processed::add);
        owner = false;

        scheduler.onEntityAdded(new EntityAddToWorldEvent(entity, world));

        assertEquals(2, nativeJobs.size());
        assertTrue(processed.isEmpty());
        owner = true;
        nativeJobs.get(1).run();
        assertEquals(3, nativeJobs.size());
        nativeJobs.get(2).run();
        assertEquals(List.of(entity), processed);
    }

    @Test
    public void foliaLoadReplacesPersistedTimerMarkerAndOldTimerCannotProcess() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        World world = world("world", new ArrayList<>());
        LivingEntity entity = entity(world);
        EntityTaskScheduler<LivingEntity> scheduler = start(candidate -> true, candidate -> true, processed::add);
        scheduler.onEntityAdded(new EntityAddToWorldEvent(entity, world));
        org.bukkit.Chunk chunk = proxy(org.bukkit.Chunk.class, (instance, method, args) -> switch (method.getName()) {
            case "getWorld" -> world;
            default -> throw new AssertionError("Unexpected chunk access: " + method.getName());
        });

        scheduler.onEntitiesLoaded(new EntitiesLoadEvent(chunk, List.of(entity)));
        nativeJobs.get(1).run();
        assertTrue(nativeJobs.get(1).cancelled);
        assertTrue(processed.isEmpty());
        nativeJobs.get(2).run();
        assertEquals(List.of(entity), processed);
    }

    @Test
    public void foliaRetirementRemovesHandleWithoutTouchingEntityState() throws ReflectiveOperationException {
        foliaField.setBoolean(null, true);
        World world = world("world", new ArrayList<>());
        LivingEntity entity = entity(world);
        EntityTaskScheduler<LivingEntity> scheduler = start(candidate -> true, candidate -> true, processed::add);
        scheduler.onEntityAdded(new EntityAddToWorldEvent(entity, world));
        owner = false;

        nativeJobs.get(1).retired.run();

        Field taskField = EntityTaskScheduler.class.getDeclaredField("nativeTasks");
        taskField.setAccessible(true);
        assertEquals(1, ((java.util.Set<?>) taskField.get(scheduler)).size());
        scheduler.close();
        assertFalse(nativeJobs.get(1).cancelled);
    }

    private EntityTaskScheduler<LivingEntity> start(Predicate<World> worldFilter, Predicate<LivingEntity> entityFilter, Consumer<LivingEntity> processor) {
        EntityTaskScheduler<LivingEntity> scheduler = EntityTaskScheduler.start(plugin, LivingEntity.class, marker, () -> enabled, worldFilter, () -> interval, entityFilter, processor);
        schedulers.add(scheduler);
        return scheduler;
    }

    private World world(String name, List<LivingEntity> entities) {
        return proxy(World.class, (instance, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "getEntitiesByClass" -> {
                scans++;
                yield entities;
            }
            default -> throw new AssertionError("Unexpected world access: " + method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private LivingEntity entity(World world) {
        Map<NamespacedKey, Object> values = new HashMap<>();
        PersistentDataContainer data = proxy(PersistentDataContainer.class, (instance, method, args) -> switch (method.getName()) {
            case "get" -> values.get(args[0]);
            case "set" -> values.put((NamespacedKey) args[0], args[2]);
            case "remove" -> values.remove(args[0]);
            default -> throw new AssertionError("Unexpected PDC access: " + method.getName());
        });
        EntityScheduler entityScheduler = proxy(EntityScheduler.class, (instance, method, args) -> switch (method.getName()) {
            case "run", "runAtFixedRate" -> newNativeJob(args[1], (Runnable) args[2]).task;
            default -> throw new AssertionError("Unexpected entity scheduler access: " + method.getName());
        });
        LivingEntity entity = proxy(LivingEntity.class, (instance, method, args) -> {
            if (method.getName().equals("getScheduler")) {
                return entityScheduler;
            }
            assertTrue("Entity state must only be read on its owner", owner);
            return switch (method.getName()) {
                case "isValid" -> true;
                case "getWorld" -> world;
                case "getPersistentDataContainer" -> data;
                default -> throw new AssertionError("Unexpected entity access: " + method.getName());
            };
        });
        ((List<LivingEntity>) world.getEntitiesByClass(LivingEntity.class)).add(entity);
        scans--;
        return entity;
    }

    @SuppressWarnings("unchecked")
    private NativeJob newNativeJob(Object action, Runnable retired) {
        NativeJob job = new NativeJob((Consumer<ScheduledTask>) action, retired);
        nativeJobs.add(job);
        return job;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> instance == args[0];
                    case "toString" -> type.getSimpleName() + " fixture";
                    default -> throw new AssertionError(method.getName());
                };
            }
            return handler.invoke(instance, method, args);
        }));
    }

    private static final class PaperJob {

        private final Runnable action;
        private final long period;
        private int cancellations;
        private final BukkitTask task = proxy(BukkitTask.class, (instance, method, args) -> {
            if (method.getName().equals("cancel")) {
                cancellations++;
                return null;
            }
            throw new AssertionError("Unexpected task access: " + method.getName());
        });

        private PaperJob(Runnable action, long period) {
            this.action = action;
            this.period = period;
        }
    }

    private static final class NativeJob {

        private final Consumer<ScheduledTask> action;
        private final Runnable retired;
        private boolean cancelled;
        private final ScheduledTask task = proxy(ScheduledTask.class, (instance, method, args) -> {
            if (method.getName().equals("cancel")) {
                cancelled = true;
                return ScheduledTask.CancelledState.CANCELLED_BY_CALLER;
            }
            throw new AssertionError("Unexpected native task access: " + method.getName());
        });

        private NativeJob(Consumer<ScheduledTask> action, Runnable retired) {
            this.action = action;
            this.retired = retired;
        }

        private void run() {
            action.accept(task);
        }
    }
}
