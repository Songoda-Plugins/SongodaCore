package com.songoda.core.nms.v1_13_R2.nbt;

import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

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
        getKeys().remove("UUID");

        Optional<EntityTypes<?>> optionalEntity = Optional.ofNullable(EntityTypes.a(entityType)); // Changed since 1.13.2
        if (optionalEntity.isPresent()) {
            Entity spawned = optionalEntity.get().spawnCreature(
                    ((CraftWorld) location.getWorld()).getHandle(),
                    compound,
                    null,
                    null,
                    new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    //EnumMobSpawn.COMMAND, // Changed since 1.13.2
                    true,
                    false,
                    CreatureSpawnEvent.SpawnReason.DEFAULT
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
    public void addExtras() {
        MinecraftKey key = IRegistry.ENTITY_TYPE.getKey(nmsEntity.P());

        if (key != null) {
            compound.setString("entity_type", key.toString()); // Changed in 1.13
        }
    }
}
