package com.songoda.core.utils;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.config.Global;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WorldUtils {

    /**
     * Checks if a given location is within the specified chunk without triggering chunk loading.
     *
     * @param location The location to check.
     * @param chunk    The chunk to compare against.
     * @return True if the location is within the chunk, false otherwise.
     */
    public static boolean isLocationAtChunk(Location location, Chunk chunk) {
        return location.getWorld().equals(chunk.getWorld()) &&
                location.getBlockX() >> 4 == chunk.getX() &&
                location.getBlockZ() >> 4 == chunk.getZ();
    }

    /**
     * Checks if the chunk at the given location is currently loaded without triggering chunk loading.
     *
     * @param location The location to check.
     * @return True if the chunk is loaded, false otherwise.
     */
    public static boolean isChunkLoadedAtLocation(Location location) {
        return location.getWorld().isChunkLoaded(
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        );
    }

    public static void queueBlockPlacement(Collection<BlockPlacement> placements, Consumer<Void> onStart, Consumer<Void> onFinish) {
        queueBlockPlacement(placements, Global.getMaxBlocksPerTick(), onStart, onFinish);
    }

    public static void queueBlockPlacement(Collection<BlockPlacement> placements, int maxBlocksPerTick, Consumer<Void> onStart, Consumer<Void> onFinish) {
        SongodaPlugin plugin = SongodaPlugin.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("SongodaPlugin instance is not initialized yet");
        }

        if (onStart != null) {
            onStart.accept(null);
        }

        if (placements.isEmpty()) {
            if (onFinish != null) {
                onFinish.accept(null);
            }
            return;
        }

        if (SchedulerUtils.isFolia()) {
            // Group by ChunkKey
            Map<ChunkKey, List<BlockPlacement>> chunkGroups = new HashMap<>();
            for (BlockPlacement bp : placements) {
                ChunkKey key = new ChunkKey(bp.getLocation());
                chunkGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(bp);
            }

            int totalChunks = chunkGroups.size();
            AtomicInteger chunksRemaining = new AtomicInteger(totalChunks);

            for (Map.Entry<ChunkKey, List<BlockPlacement>> entry : chunkGroups.entrySet()) {
                List<BlockPlacement> chunkPlacements = entry.getValue();
                if (chunkPlacements.isEmpty()) {
                    if (chunksRemaining.decrementAndGet() == 0) {
                        if (onFinish != null) {
                            SchedulerUtils.runTask(plugin, () -> onFinish.accept(null));
                        }
                    }
                    continue;
                }

                Location scheduleLoc = chunkPlacements.get(0).getLocation();
                Queue<BlockPlacement> chunkQueue = new LinkedList<>(chunkPlacements);

                SchedulerUtils.runLocationTask(plugin, scheduleLoc, new Runnable() {
                    @Override
                    public void run() {
                        int placed = 0;
                        while (placed < maxBlocksPerTick && !chunkQueue.isEmpty()) {
                            BlockPlacement bp = chunkQueue.poll();
                            if (bp != null) {
                                applyPlacement(bp);
                            }
                            placed++;
                        }

                        if (chunkQueue.isEmpty()) {
                            if (chunksRemaining.decrementAndGet() == 0) {
                                if (onFinish != null) {
                                    SchedulerUtils.runTask(plugin, () -> onFinish.accept(null));
                                }
                            }
                        } else {
                            SchedulerUtils.runLocationTaskLater(plugin, scheduleLoc, this, 1L);
                        }
                    }
                });
            }
        } else {
            // Standard Paper-compatible server
            Queue<BlockPlacement> queue = new LinkedList<>(placements);

            SchedulerUtils.runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    int placed = 0;
                    while (placed < maxBlocksPerTick && !queue.isEmpty()) {
                        BlockPlacement bp = queue.poll();
                        if (bp != null) {
                            applyPlacement(bp);
                        }
                        placed++;
                    }

                    if (queue.isEmpty()) {
                        if (onFinish != null) {
                            onFinish.accept(null);
                        }
                    } else {
                        SchedulerUtils.runTaskLater(plugin, this, 1L);
                    }
                }
            });
        }
    }

    private static void applyPlacement(BlockPlacement bp) {
        Location loc = bp.getLocation();
        if (bp.getBlockData() != null) {
            loc.getBlock().setBlockData(bp.getBlockData(), false);
        } else if (bp.getMaterial() != null) {
            loc.getBlock().setType(bp.getMaterial(), false);
        }
    }

    public static class BlockPlacement {
        private final Location location;
        private final Material material;
        private final BlockData blockData;

        public BlockPlacement(Location location, Material material) {
            this.location = location;
            this.material = material;
            this.blockData = null;
        }

        public BlockPlacement(Location location, BlockData blockData) {
            this.location = location;
            this.blockData = blockData;
            this.material = blockData.getMaterial();
        }

        public Location getLocation() {
            return location;
        }

        public Material getMaterial() {
            return material;
        }

        public BlockData getBlockData() {
            return blockData;
        }
    }

    private static class ChunkKey {
        private final org.bukkit.World world;
        private final int x;
        private final int z;

        public ChunkKey(Location loc) {
            this.world = loc.getWorld();
            this.x = loc.getBlockX() >> 4;
            this.z = loc.getBlockZ() >> 4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkKey chunkKey = (ChunkKey) o;
            return x == chunkKey.x && z == chunkKey.z && world.equals(chunkKey.world);
        }

        @Override
        public int hashCode() {
            int result = world.hashCode();
            result = 31 * result + x;
            result = 31 * result + z;
            return result;
        }
    }
}
