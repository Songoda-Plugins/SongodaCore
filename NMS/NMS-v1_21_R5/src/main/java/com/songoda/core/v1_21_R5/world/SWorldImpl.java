package com.songoda.core.v1_21_R5.world;

import com.songoda.core.nms.world.SWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
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
        List<LivingEntity> result = new ArrayList<>();

        ServerLevel worldServer = ((CraftWorld) this.world).getHandle();
        LevelEntityGetter<Entity> entities = worldServer.getEntities();

        entities.getAll().forEach((mcEnt) -> {
            org.bukkit.entity.Entity bukkitEntity = mcEnt.getBukkitEntity();

            if (bukkitEntity instanceof LivingEntity && bukkitEntity.isValid()) {
                result.add((LivingEntity) bukkitEntity);
            }
        });

        return result;
    }

    @Override
    public void setBlockFast(int x, int y, int z, Material material) {
        throw new RuntimeException("Not implemented yet");
    }
}
