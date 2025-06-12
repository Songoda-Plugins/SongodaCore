package com.songoda.core.nms.v1_8_R3.world;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.v1_8_R3.world.spawner.BBaseSpawnerImpl;
import com.songoda.core.nms.world.BBaseSpawner;
import com.songoda.core.nms.world.SItemStack;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SWorld;
import com.songoda.core.nms.world.WorldCore;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockButtonAbstract;
import net.minecraft.server.v1_8_R3.BlockLever;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.MobSpawnerAbstract;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
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
        Object cTileEntity = ReflectionUtils.getFieldValue(spawner, "spawner");

        return new BBaseSpawnerImpl((MobSpawnerAbstract) ReflectionUtils.getFieldValue(cTileEntity, "a"));
    }

    /**
     * Method is based on {@link WorldServer#h()}.
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) throws NoSuchFieldException, IllegalAccessException {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();

        if (tickAmount <= 0) {
            return;
        }

        int k = chunk.locX * 16;
        int l = chunk.locZ * 16;

        for (ChunkSection chunksection : chunk.getSections()) {
            if (chunksection != null && chunksection.shouldTick()) {
                for (int l1 = 0; l1 < tickAmount; ++l1) {
                    int m = (int) ReflectionUtils.getFieldValue(chunk.world, "m");

                    m = m * 3 + 1013904223;
                    ReflectionUtils.setFieldValue(chunk.world, "m", m);

                    int i2 = m >> 2;
                    int j2 = i2 & 15;
                    int k2 = i2 >> 8 & 15;
                    int l2 = i2 >> 16 & 15;

                    IBlockData iblockdata = chunksection.getType(j2, l2, k2);
                    Block block = iblockdata.getBlock();

                    if (block.isTicking()) {
                        block.a(chunk.world, new BlockPosition(j2 + k, l2 + chunksection.getYPosition(), k2 + l), iblockdata, chunk.world.random);
                    }
                }
            }
        }
    }

    @Override
    public void updateAdjacentComparators(@NotNull org.bukkit.block.Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;
        WorldServer serverLevel = ((CraftWorld) craftBlock.getWorld()).getHandle();

        BlockPosition blockPos = new BlockPosition(craftBlock.getX(), craftBlock.getY(), craftBlock.getZ());
        Block nmsBlock = CraftMagicNumbers.getBlock(bukkitBlock.getType());
        serverLevel.updateAdjacentComparators(blockPos, nmsBlock);
    }

    @Override
    public void toggleLever(@NotNull org.bukkit.block.Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        BlockLever leverBlock = (BlockLever) CraftMagicNumbers.getBlock(craftBlock);
        IBlockData iBlockData = leverBlock.getBlockData();
        BlockPosition blockposition = new BlockPosition(craftBlock.getX(), craftBlock.getY(), craftBlock.getZ());
        WorldServer world = ((CraftWorld) craftBlock.getWorld()).getHandle();

        leverBlock.interact(world, blockposition, iBlockData, null, null, 0, 0, 0);
    }

    @Override
    public void pressButton(@NotNull org.bukkit.block.Block bukkitBlock) {
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;

        BlockButtonAbstract buttonBlock = (BlockButtonAbstract) CraftMagicNumbers.getBlock(craftBlock);
        IBlockData iBlockData = buttonBlock.getBlockData();
        BlockPosition blockposition = new BlockPosition(craftBlock.getX(), craftBlock.getY(), craftBlock.getZ());
        WorldServer world = ((CraftWorld) craftBlock.getWorld()).getHandle();

        buttonBlock.interact(world, blockposition, iBlockData, null, null, 0, 0, 0);
    }
}
