package com.songoda.core.nms.v1_19_R1.nbt;

import com.songoda.core.nms.nbt.NBTCore;
import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class NBTCoreImpl implements NBTCore {
    @Override
    public NBTEntity of(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        CompoundTag nbt = new CompoundTag();
        nmsEntity.saveWithoutId(nbt);

        return new NBTEntityImpl(nbt, nmsEntity);
    }

    @Override
    public NBTEntity newEntity() {
        return new NBTEntityImpl(new CompoundTag(), null);
    }
}
