package com.songoda.core.nms.v1_10_R1.world;

import com.songoda.core.nms.world.SWorld;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.WorldServer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
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

        levelChunk.a(new BlockPosition(x & 0xF, y, z & 0xF), Block.getByCombinedId(0));
    }
}
