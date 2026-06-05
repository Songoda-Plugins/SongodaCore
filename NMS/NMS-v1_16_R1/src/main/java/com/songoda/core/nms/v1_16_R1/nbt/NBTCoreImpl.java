package com.songoda.core.nms.v1_16_R1.nbt;

import com.songoda.core.nms.nbt.NBTCore;
import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class NBTCoreImpl implements NBTCore {
    @Override
    public NBTEntity of(Entity entity) {
        net.minecraft.server.v1_16_R1.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        NBTTagCompound nbt = new NBTTagCompound();
        nmsEntity.save(nbt);
        return new NBTEntityImpl(nbt, nmsEntity);
    }

    @Override
    public NBTEntity newEntity() {
        return new NBTEntityImpl(new NBTTagCompound(), null);
    }
}
