package com.songoda.core.nms.v1_8_R2.entity;

import com.songoda.core.nms.entity.NmsEntity;
import net.minecraft.server.v1_8_R2.Entity;
import net.minecraft.server.v1_8_R2.EntityInsentient;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;

public class NmsEntityImpl implements NmsEntity {
    @Override
    public boolean isMob(org.bukkit.entity.Entity entity) {
        return ((CraftEntity) entity).getHandle() instanceof EntityInsentient;
    }

    @Override
    public void setMobAware(org.bukkit.entity.Entity entity, boolean aware) {
        if (!isMob(entity)) {
            throw new IllegalArgumentException("Entity is not a mob and cannot be set aware");
        }

        Entity nmsEntity = ((CraftEntity) entity).getHandle();
        nmsEntity.fromMobSpawner = aware;
    }

    @Override
    public boolean isAware(org.bukkit.entity.Entity entity) {
        Entity nmsEntity = ((CraftEntity) entity).getHandle();
        if (nmsEntity instanceof EntityInsentient) {
            return nmsEntity.fromMobSpawner;
        }

        return false;
    }
}
