package com.songoda.core.nms.v1_10_R1.nbt;

import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.server.v1_10_R1.Entity;
import net.minecraft.server.v1_10_R1.EntityTypes;
import net.minecraft.server.v1_10_R1.ItemMonsterEgg;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

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

        Entity spawned = ItemMonsterEgg.spawnCreature( // Changed since 1.14
                ((CraftWorld) location.getWorld()).getHandle(),
                entityType, // Parameter simplified in 1.10
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
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

        return null;
    }

    @Override
    public org.bukkit.entity.Entity reSpawn(Location location) {
        nmsEntity.dead = true;

        return spawn(location);
    }

    @Override
    public void addExtras() {
        String key = EntityTypes.b(nmsEntity); // Changed in 1.12

        if (key != null) {
            compound.setString("entity_type", key); // Changed in 1.13
        }
    }
}
