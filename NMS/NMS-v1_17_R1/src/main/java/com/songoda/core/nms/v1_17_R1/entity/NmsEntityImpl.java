package com.songoda.core.nms.v1_17_R1.entity;

import com.songoda.core.nms.entity.NmsEntity;
import net.minecraft.world.entity.EntityInsentient;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class NmsEntityImpl implements NmsEntity {
    @Override
    public boolean isMob(Entity entity) {
        return ((CraftEntity) entity).getHandle() instanceof EntityInsentient;
    }

    @Override
    public void setMobAware(Entity entity, boolean aware) {
        if (!isMob(entity)) {
            throw new IllegalArgumentException("Entity is not a mob and cannot be set aware");
        }

        var nmsEntity = ((CraftEntity) entity).getHandle();
        ((EntityInsentient) nmsEntity).aware = aware;
    }

    @Override
    public boolean isAware(Entity entity) {
        var nmsEntity = ((CraftEntity) entity).getHandle();
        if (nmsEntity instanceof EntityInsentient nmsMob) {
            return nmsMob.aware;
        }

        return false;
    }
}
