package com.songoda.core.nms.v1_15_R1.nbt;

import com.songoda.core.nms.nbt.NBTCompound;
import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.EnumMobSpawn;
import net.minecraft.server.v1_15_R1.IRegistry;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;

import java.util.Optional;

public class NBTEntityImpl extends NBTCompoundImpl implements NBTEntity {
    private Entity nmsEntity;

    public NBTEntityImpl(NBTTagCompound entityNBT, Entity nmsEntity) {
        super(entityNBT);

        this.nmsEntity = nmsEntity;
    }

    @Override
    public org.bukkit.entity.Entity spawn(Location location) {
        String entityType = getNBTObject("entity_type").asString();

        Optional<EntityTypes<?>> optionalEntity = EntityTypes.a(entityType);
        if (optionalEntity.isPresent()) {
            assert location.getWorld() != null;

            Entity spawned = optionalEntity.get().spawnCreature(
                    ((CraftWorld) location.getWorld()).getHandle(),
                    compound,
                    null,
                    null,
                    new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    EnumMobSpawn.COMMAND,
                    true,
                    false
            );

            if (spawned != null) {
                spawned.f(compound); // This changed from 1.16.1
                org.bukkit.entity.Entity entity = spawned.getBukkitEntity();
                spawned.setLocation(location.getX(), location.getY(), location.getZ(),
                        location.getPitch(), location.getYaw());
                nmsEntity = spawned;

                return entity;
            }
        }

        return null;
    }

    @Override
    public org.bukkit.entity.Entity reSpawn(Location location) {
        nmsEntity.dead = true;

        return spawn(location);
    }

    @Override
    public NBTCompound set(String tag, byte[] b) {
        compound.setByteArray(tag, b);

        return this;
    }

    @Override
    public byte[] getByteArray(String tag) {
        return new byte[0];
    }

    @Override
    public void addExtras() {
        compound.setString("entity_type", IRegistry.ENTITY_TYPE.getKey(nmsEntity.getEntityType()).toString());
    }
}
