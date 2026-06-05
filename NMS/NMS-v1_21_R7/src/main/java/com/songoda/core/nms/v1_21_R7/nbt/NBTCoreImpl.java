package com.songoda.core.nms.v1_21_R7.nbt;

import com.songoda.core.nms.nbt.NBTCore;
import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class NBTCoreImpl implements NBTCore {
    @Override
    public NBTEntity of(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        TagValueOutput tagValueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        nmsEntity.saveWithoutId(tagValueOutput);

        CompoundTag nbt = new CompoundTag();

        return new NBTEntityImpl(nbt, nmsEntity);
    }

    @Override
    public NBTEntity newEntity() {
        return new NBTEntityImpl(new CompoundTag(), null);
    }
}
