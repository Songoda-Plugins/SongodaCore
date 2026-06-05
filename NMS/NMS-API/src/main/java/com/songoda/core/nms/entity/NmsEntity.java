package com.songoda.core.nms.entity;

import org.bukkit.entity.Entity;

public interface NmsEntity {
    boolean isMob(Entity entity);

    void setMobAware(Entity entity, boolean aware);

    boolean isAware(Entity entity);
}
