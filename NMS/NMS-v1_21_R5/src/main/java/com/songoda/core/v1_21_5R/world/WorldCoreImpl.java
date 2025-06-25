package com.songoda.core.v1_21_5R.world;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.world.BBaseSpawner;
import com.songoda.core.nms.world.SItemStack;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SWorld;
import com.songoda.core.nms.world.WorldCore;
import com.songoda.core.v1_21_5R.world.spawner.BBaseSpawnerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_21_R5.CraftChunk;
import org.bukkit.craftbukkit.v1_21_R5.block.CraftBlock;
import org.bukkit.craftbukkit.v1_21_R5.block.data.CraftBlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        Object nmsBlockEntity;
        try {
            Method getBlockEntity = spawner.getClass().getMethod("getBlockEntity");
            nmsBlockEntity = getBlockEntity.invoke(spawner);
        } catch (NoSuchMethodException | InvocationTargetException ex) {
            // fallback for non-Paper servers
            nmsBlockEntity = ReflectionUtils.getFieldValue(spawner, "tileEntity");
        }

        return new BBaseSpawnerImpl(spawner, ((SpawnerBlockEntity) nmsBlockEntity).getSpawner());
    }

    /**
     * Method is based on {@link ServerLevel#tickChunk(LevelChunk, int)}.
     */
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) {
        LevelChunk chunk = (LevelChunk) ((CraftChunk) bukkitChunk).getHandle(ChunkStatus.FULL);
        ServerLevel world = chunk.r;
        ChunkPos chunkCoordIntPair = chunk.getPos();
        int j = chunkCoordIntPair.getMinBlockX();
        int k = chunkCoordIntPair.getMinBlockZ();

        ProfilerFiller gameprofilerfiller = Profiler.get();

        gameprofilerfiller.push("tickBlocks");
        if (tickAmount > 0) {
            LevelChunkSection[] aChunkSection = chunk.getSections();

            for (int i1 = 0; i1 < aChunkSection.length; ++i1) {
                LevelChunkSection chunkSection = aChunkSection[i1];
                if (chunkSection.isRandomlyTicking()) {
                    int j1 = chunk.getSectionYFromSectionIndex(i1);
                    int k1 = SectionPos.sectionToBlockCoord(j1);

                    for (int l1 = 0; l1 < tickAmount; ++l1) {
                        BlockPos blockposition = world.getBlockRandomPos(j, k1, k, 15);
                        gameprofilerfiller.push("randomTick");
                        BlockState iBlockData = chunkSection.getBlockState(blockposition.getX() - j, blockposition.getY() - k1, blockposition.getZ() - k);
                        if (iBlockData.isRandomlyTicking()) {
                            iBlockData.randomTick(world, blockposition, world.random);
                        }

                        FluidState fluid = iBlockData.getFluidState();
                        if (fluid.isRandomlyTicking()) {
                            fluid.randomTick(world, blockposition, world.random);
                        }

                        gameprofilerfiller.pop();
                    }
                }
            }
        }

        gameprofilerfiller.pop();
    }

    @Override
    public void updateAdjacentComparators(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;
        ServerLevel serverLevel = craftBlock.getCraftWorld().getHandle();

        serverLevel.updateNeighbourForOutputSignal(craftBlock.getPosition(), craftBlock.getNMS().getBlock());
    }

    @Override
    public void toggleLever(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        BlockState iBlockData = ((CraftBlockData) craftBlock.getBlockData()).getState();
        BlockPos blockposition = craftBlock.getPosition();
        ServerLevel world = craftBlock.getCraftWorld().getHandle();

        ((LeverBlock) craftBlock.getNMS().getBlock()).pull(iBlockData, world, blockposition, null);
    }

    @Override
    public void pressButton(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        BlockState iBlockData = ((CraftBlockData) craftBlock.getBlockData()).getState();
        BlockPos blockposition = craftBlock.getPosition();
        ServerLevel world = craftBlock.getCraftWorld().getHandle();

        ((ButtonBlock) craftBlock.getNMS().getBlock()).press(iBlockData, world, blockposition, null);
    }
}
