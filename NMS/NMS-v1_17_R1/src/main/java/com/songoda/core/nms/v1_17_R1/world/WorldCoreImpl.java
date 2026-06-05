package com.songoda.core.nms.v1_17_R1.world;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.v1_17_R1.world.spawner.BBaseSpawnerImpl;
import com.songoda.core.nms.world.BBaseSpawner;
import com.songoda.core.nms.world.SItemStack;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SWorld;
import com.songoda.core.nms.world.WorldCore;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.MobSpawnerAbstract;
import net.minecraft.world.level.block.BlockButtonAbstract;
import net.minecraft.world.level.block.BlockLever;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.material.Fluid;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
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

        return new BBaseSpawnerImpl(spawner, (MobSpawnerAbstract) ReflectionUtils.getFieldValue(cTileEntity, "a"));
    }

    /**
     * Method is based on {@link WorldServer#a(Chunk, int)}.
     */
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();
        WorldServer world = (WorldServer) chunk.getWorld();

        if (tickAmount <= 0) {
            return;
        }

        GameProfilerFiller profiler = world.getMethodProfiler();

        ChunkCoordIntPair chunkPos = chunk.getPos();
        int minBlockX = chunkPos.d();
        int minBlockZ = chunkPos.e();

        profiler.enter("tickBlocks");
        for (ChunkSection cSection : chunk.getSections()) {
            if (cSection != Chunk.a && // cSection != Chunk.EMPTY_SECTION
                    cSection.d()) {  // #isRandomlyTicking
                int bottomBlockY = cSection.getYPosition();

                for (int k1 = 0; k1 < tickAmount; ++k1) {
                    BlockPosition bPos = world.a(minBlockX, bottomBlockY, minBlockZ, 15);
                    profiler.enter("randomTick");

                    IBlockData blockState = cSection.getType(bPos.getX() - minBlockX, bPos.getY() - bottomBlockY, bPos.getZ() - minBlockZ);

                    if (blockState.isTicking()) {
                        blockState.b(world, bPos, chunk.getWorld().w);  // #randomTick
                    }

                    Fluid fluid = blockState.getFluid();
                    if (fluid.f()) {    // #isRandomlyTicking
                        fluid.b(world, bPos, chunk.getWorld().w);  // #randomTick
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
