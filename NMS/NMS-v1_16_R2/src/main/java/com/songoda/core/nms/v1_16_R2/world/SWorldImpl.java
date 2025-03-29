package com.songoda.core.nms.v1_16_R2.world;

import com.songoda.core.nms.world.SWorld;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.Chunk;
import net.minecraft.server.v1_16_R2.IBlockData;
import net.minecraft.server.v1_16_R2.WorldServer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class SWorldImpl implements SWorld {
    private final World world;

    public SWorldImpl(World world) {
        this.world = world;
    }

    @Override
    public List<LivingEntity> getLivingEntities() {
        return new ArrayList<>();   // FIXME
    }

    @Override
    public void setBlockFast(int x, int y, int z, Material material) {
        WorldServer serverLevel = ((CraftWorld) this.world).getHandle();
        Chunk levelChunk = serverLevel.getChunkIfLoaded(x >> 4, z >> 4);
        IBlockData blockState = ((CraftBlockData) material.createBlockData()).getState();

        levelChunk.setType(new BlockPosition(x & 0xF, y, z & 0xF), blockState, true);
    }
}
