package com.songoda.core.nms.world;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface WorldCore {
    /**
     * @deprecated Use {@link #getSpawner(Location)} instead
     */
    @Deprecated
    SSpawner getSpawner(CreatureSpawner spawner);

    SSpawner getSpawner(Location location);

    SItemStack getItemStack(ItemStack item);

    SWorld getWorld(World world);

    BBaseSpawner getBaseSpawner(CreatureSpawner spawner) throws ReflectiveOperationException;

    /**
     * Performs random ticks on a specific chunks.
     * <br><br>
     * More information: <a href="https://minecraft.fandom.com/wiki/Tick#Random_tick">https://minecraft.fandom.com/wiki/Tick#Random_tick</a>
     *
     * @param bukkitChunk The chunk to tick
     * @param tickAmount  The number of blocks to tick per ChunkSection, normally referred to as <code>randomTickSpeed</code>
     */
    void randomTickChunk(Chunk bukkitChunk, int tickAmount) throws ReflectiveOperationException;

    void updateAdjacentComparators(@NotNull Block bukkitBlock);

    void toggleLever(@NotNull Block bukkitBlock);

    void pressButton(@NotNull Block bukkitBlock);

    /**
     * Ticks all inactive spawners in a specific chunk ignoring the minimum required players within a specific range.<br>
     * A spawner is deemed inactive if no player is within its activation range.
     *
     * @param chunk  The chunk to tick the spawners in
     * @param amount The amount of ticks to execute for each spawner
     */
    default void tickInactiveSpawners(Chunk chunk, int amount) throws ReflectiveOperationException {
        if (amount <= 0) return;

        for (BlockState tileEntity : chunk.getTileEntities()) {
            if (tileEntity instanceof CreatureSpawner) {
                BBaseSpawner spawner = getBaseSpawner((CreatureSpawner) tileEntity);

                if (!spawner.isNearPlayer()) {
                    for (int i = 0; i < amount; ++i) {
                        spawner.tick();
                    }
                }
            }
        }
    }
}
