package com.songoda.core.nms.nbt;

import org.bukkit.entity.Entity;

public interface NBTCore {
    NBTEntity of(Entity entity);

    NBTEntity newEntity();
}
