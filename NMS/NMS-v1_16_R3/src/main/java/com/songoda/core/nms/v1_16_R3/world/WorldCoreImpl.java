package com.songoda.core.nms.v1_16_R3.world;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.v1_16_R3.world.spawner.BBaseSpawnerImpl;
import com.songoda.core.nms.world.BBaseSpawner;
import com.songoda.core.nms.world.SItemStack;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SWorld;
import com.songoda.core.nms.world.WorldCore;
import net.minecraft.server.v1_16_R3.BlockButtonAbstract;
import net.minecraft.server.v1_16_R3.BlockLever;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkSection;
import net.minecraft.server.v1_16_R3.Fluid;
import net.minecraft.server.v1_16_R3.GameProfilerFiller;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MobSpawnerAbstract;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WorldCoreImpl implements WorldCore {
    @Override
    public SSpawner getSpawner(CreatureSpawner spawner) {
        return new SSpawnerImpl(spawner.getLocation());
    }

    @Override
    public SSpawner getSpawner(Location location) {
        return new SSpawnerImpl(location);
    }

    @Override
    public SItemStack getItemStack(ItemStack item) {
        return new SItemStackImpl(item);
    }

    @Override
    public SWorld getWorld(World world) {
        return new SWorldImpl(world);
    }

    @Override
    public BBaseSpawner getBaseSpawner(CreatureSpawner spawner) throws NoSuchFieldException, IllegalAccessException {
        Object cTileEntity = ReflectionUtils.getFieldValue(spawner, "tileEntity");

        return new BBaseSpawnerImpl((MobSpawnerAbstract) ReflectionUtils.getFieldValue(cTileEntity, "a"));
    }

    /**
     * Method is based on {@link WorldServer#a(Chunk, int)}.
     */
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();

        if (tickAmount <= 0) {
            return;
        }

        GameProfilerFiller profiler = chunk.world.getMethodProfiler();

        ChunkCoordIntPair chunkPos = chunk.getPos();
        int minBlockX = chunkPos.d();
        int minBlockZ = chunkPos.e();

        profiler.enter("tickBlocks");
        for (ChunkSection cSection : chunk.getSections()) {
            if (cSection != Chunk.a &&    // cSection != Chunk.EMPTY_SECTION
                    cSection.d()) { // #isRandomlyTicking()
                int bottomBlockY = cSection.getYPosition();

                for (int i = 0; i < tickAmount; ++i) {
                    BlockPosition randomBlockPos = chunk.world.a(minBlockX, bottomBlockY, minBlockZ, 15);   // getBlockRandomPos
                    profiler.enter("randomTick");

                    IBlockData blockState = cSection.getType(
                            randomBlockPos.getX() - minBlockX,
                            randomBlockPos.getY() - bottomBlockY,
                            randomBlockPos.getZ() - minBlockZ);   // #getBlockState

                    if (blockState.isTicking()) {   // #isRandomlyTicking()
                        blockState.b(chunk.world, randomBlockPos, chunk.world.random);  // #randomTick
                    }

                    Fluid fluidState = blockState.getFluid();   // #getFluidState()
                    if (fluidState.f()) {   // #isRandomlyTicking()
                        fluidState.b(chunk.world, randomBlockPos, chunk.world.random);  // #randomTick
                    }

                    profiler.exit();
                }
            }
        }

        profiler.exit();
    }

    @Override
    public void updateAdjacentComparators(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;
        WorldServer serverLevel = craftBlock.getCraftWorld().getHandle();

        serverLevel.updateAdjacentComparators(craftBlock.getPosition(), craftBlock.getNMS().getBlock());
    }

    @Override
    public void toggleLever(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        IBlockData iBlockData = ((CraftBlockData) craftBlock.getBlockData()).getState();
        BlockPosition blockposition = craftBlock.getPosition();
        WorldServer world = craftBlock.getCraftWorld().getHandle();

        ((BlockLever) craftBlock.getNMS().getBlock()).d(iBlockData, world, blockposition);
    }

    @Override
    public void pressButton(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        IBlockData iBlockData = ((CraftBlockData) craftBlock.getBlockData()).getState();
        BlockPosition blockposition = craftBlock.getPosition();
        WorldServer world = craftBlock.getCraftWorld().getHandle();

        ((BlockButtonAbstract) craftBlock.getNMS().getBlock()).d(iBlockData, world, blockposition);
    }
}
