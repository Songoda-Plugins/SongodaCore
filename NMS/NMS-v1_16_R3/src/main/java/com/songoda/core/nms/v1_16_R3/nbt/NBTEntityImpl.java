package com.songoda.core.nms.v1_16_R3.nbt;

import com.songoda.core.nms.nbt.NBTEntity;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumMobSpawn;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

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
                spawned.load(compound);
                org.bukkit.entity.Entity entity = spawned.getBukkitEntity();
                entity.teleport(location);
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
        compound.setString("entity_type", IRegistry.ENTITY_TYPE.getKey(nmsEntity.getEntityType()).toString());
    }
}
