package com.songoda.core.world;

import com.songoda.core.compatibility.folia.SchedulerUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Queries living entities across region boundaries while accessing each chunk on its owning region.
 */
public final class RegionEntityQuery {

    private RegionEntityQuery() {
    }

    /**
     * Counts nearby living entities, excluding players and armor stands.
     *
     * @param plugin plugin used to schedule region tasks
     * @param center center of the search area
     * @param radiusX horizontal X radius
     * @param radiusY vertical radius
     * @param radiusZ horizontal Z radius
     * @param resultConsumer receives the count, or {@code -1} if any region query fails
     */
    public static void countNearbyLivingEntities(
            @NotNull Plugin plugin,
            @NotNull Location center,
            int radiusX,
            int radiusY,
            int radiusZ,
            @NotNull Consumer<Integer> resultConsumer
    ) {
        countNearbyLivingEntities(plugin, center, radiusX, radiusY, radiusZ, entity -> 1, resultConsumer);
    }

    /**
     * Counts nearby living entities using a caller-supplied weight for each entity.
     *
     * @param plugin plugin used to schedule region tasks
     * @param center center of the search area
     * @param radiusX horizontal X radius
     * @param radiusY vertical radius
     * @param radiusZ horizontal Z radius
     * @param entityWeight returns the count represented by an entity
     * @param resultConsumer receives the count, or {@code -1} if any region query fails
     */
    public static void countNearbyLivingEntities(
            @NotNull Plugin plugin,
            @NotNull Location center,
            int radiusX,
            int radiusY,
            int radiusZ,
            @NotNull ToIntFunction<LivingEntity> entityWeight,
            @NotNull Consumer<Integer> resultConsumer
    ) {
        World world = center.getWorld();
        if (world == null) {
            resultConsumer.accept(0);
            return;
        }

        int minChunkX = Math.floorDiv((int) Math.floor(center.getX() - Math.abs(radiusX)), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(center.getX() + Math.abs(radiusX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(center.getZ() - Math.abs(radiusZ)), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(center.getZ() + Math.abs(radiusZ)), 16);
        int taskCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        AtomicInteger remainingTasks = new AtomicInteger(taskCount);
        AtomicInteger entityCount = new AtomicInteger();
        AtomicBoolean queryFailed = new AtomicBoolean();
        BoundingBox searchArea = new BoundingBox(
                center.getX() - Math.abs(radiusX),
                center.getY() - Math.abs(radiusY),
                center.getZ() - Math.abs(radiusZ),
                center.getX() + Math.abs(radiusX),
                center.getY() + Math.abs(radiusY),
                center.getZ() + Math.abs(radiusZ)
        );

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int queryChunkX = chunkX;
                int queryChunkZ = chunkZ;
                Location taskLocation = new Location(world, chunkX * 16.0 + 8.0, center.getY(), chunkZ * 16.0 + 8.0);
                Runnable query = () -> {
                    try {
                        if (world.isChunkLoaded(queryChunkX, queryChunkZ)) {
                            for (Entity entity : world.getChunkAt(queryChunkX, queryChunkZ).getEntities()) {
                                if (entity instanceof LivingEntity livingEntity
                                        && !(entity instanceof Player)
                                        && entity.getType() != EntityType.ARMOR_STAND
                                        && entity.getBoundingBox().overlaps(searchArea)) {
                                    entityCount.addAndGet(entityWeight.applyAsInt(livingEntity));
                                }
                            }
                        }
                    } catch (RuntimeException exception) {
                        queryFailed.set(true);
                    } finally {
                        if (remainingTasks.decrementAndGet() == 0) {
                            Location callbackLocation = center.clone();
                            SchedulerUtils.runLocationTask(plugin, callbackLocation,
                                    () -> resultConsumer.accept(queryFailed.get() ? -1 : entityCount.get()));
                        }
                    }
                };
                try {
                    SchedulerUtils.runLocationTask(plugin, taskLocation, query);
                } catch (RuntimeException exception) {
                    queryFailed.set(true);
                    if (remainingTasks.decrementAndGet() == 0) {
                        SchedulerUtils.runLocationTask(plugin, center,
                                () -> resultConsumer.accept(-1));
                    }
                }
            }
        }
    }
}
