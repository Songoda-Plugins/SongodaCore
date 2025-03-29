package com.songoda.core.nms.v1_18_R1.world;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.v1_18_R1.world.spawner.BBaseSpawnerImpl;
import com.songoda.core.nms.world.BBaseSpawner;
import com.songoda.core.nms.world.SItemStack;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SWorld;
import com.songoda.core.nms.world.WorldCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_18_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
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

        return new BBaseSpawnerImpl(spawner, (BaseSpawner) ReflectionUtils.getFieldValue(cTileEntity, "a"));
    }

    /**
     * Method is based on {@link ServerLevel#tickChunk(LevelChunk, int)}.
     */
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) {
        LevelChunk chunk = ((CraftChunk) bukkitChunk).getHandle();
        ServerLevel world = chunk.q;

        ChunkPos chunkcoordintpair = chunk.getPos();
        int j = chunkcoordintpair.getMinBlockX();
        int k = chunkcoordintpair.getMinBlockZ();

        ProfilerFiller gameprofilerfiller = world.getProfiler();
        gameprofilerfiller.popPush("tickBlocks");
        if (tickAmount > 0) {
            LevelChunkSection[] achunksection = chunk.getSections();
            int l = achunksection.length;

            for (LevelChunkSection chunksection : achunksection) {
                if (chunksection.isRandomlyTicking()) {
                    int j1 = chunksection.bottomBlockY();

                    for (int k1 = 0; k1 < tickAmount; ++k1) {
                        BlockPos blockposition2 = world.getBlockRandomPos(j, j1, k, 15);
                        gameprofilerfiller.push("randomTick");
                        BlockState iblockdata1 = chunksection.getBlockState(blockposition2.getX() - j, blockposition2.getY() - j1, blockposition2.getZ() - k);
                        if (iblockdata1.isRandomlyTicking()) {
                            iblockdata1.randomTick(world, blockposition2, world.random);
                        }

                        FluidState fluid = iblockdata1.getFluidState();
                        if (fluid.isRandomlyTicking()) {
                            fluid.randomTick(world, blockposition2, world.random);
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

        ((LeverBlock) craftBlock.getNMS().getBlock()).pull(iBlockData, world, blockposition);
    }

    @Override
    public void pressButton(@NotNull Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        BlockState iBlockData = ((CraftBlockData) craftBlock.getBlockData()).getState();
        BlockPos blockposition = craftBlock.getPosition();
        ServerLevel world = craftBlock.getCraftWorld().getHandle();

        ((ButtonBlock) craftBlock.getNMS().getBlock()).press(iBlockData, world, blockposition);
    }
}
