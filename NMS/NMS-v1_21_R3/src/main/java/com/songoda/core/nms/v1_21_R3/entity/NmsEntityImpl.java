package com.songoda.core.nms.v1_21_R3.entity;

import com.songoda.core.nms.entity.NmsEntity;
import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class NmsEntityImpl implements NmsEntity {
    @Override
    public boolean isMob(Entity entity) {
        return ((CraftEntity) entity).getHandle() instanceof Mob;
    }

    @Override
    public void setMobAware(Entity entity, boolean aware) {
        if (!isMob(entity)) {
            throw new IllegalArgumentException("Entity is not a mob and cannot be set aware");
        }

        var nmsEntity = ((CraftEntity) entity).getHandle();
        ((Mob) nmsEntity).aware = aware;
    }

    @Override
    public boolean isAware(Entity entity) {
        var nmsEntity = ((CraftEntity) entity).getHandle();
        if (nmsEntity instanceof Mob nmsMob) {
            return nmsMob.aware;
        }

        return false;
    }
}
