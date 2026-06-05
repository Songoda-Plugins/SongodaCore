package com.songoda.core.nms.v1_8_R2.world;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SpawnedEntity;
import com.cryptomorin.xseries.XMaterial;
import net.minecraft.server.v1_8_R2.BlockPosition;
import net.minecraft.server.v1_8_R2.DifficultyDamageScaler;
import net.minecraft.server.v1_8_R2.Entity;
import net.minecraft.server.v1_8_R2.EntityInsentient;
import net.minecraft.server.v1_8_R2.EntityTypes;
import net.minecraft.server.v1_8_R2.WorldServer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class SSpawnerImpl implements SSpawner {
    private final Location spawnerLocation;

    public SSpawnerImpl(Location location) {
        this.spawnerLocation = location;
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, Location spawnerLocation) {
        return spawnEntity(type, "EXPLOSION_NORMAL", null, null);
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, String particleType, SpawnedEntity spawned, Set<XMaterial> canSpawnOn) {
        short spawnRange = 4;
        for (int i = 0; i < 50; ++i) {
            WorldServer world = ((CraftWorld) this.spawnerLocation.getWorld()).getHandle();

            Random random = world.random;
            double x = this.spawnerLocation.getX() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;
            double y = this.spawnerLocation.getY() + random.nextInt(3) - 1;
            double z = this.spawnerLocation.getZ() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;

            Entity entity = EntityTypes.createEntityByName(translateName(type, true), world);
            entity.setPositionRotation(x, y, z, 360.0F, 0.0F);

            BlockPosition position = entity.getChunkCoordinates();
            DifficultyDamageScaler damageScaler = world.E(position);

            if (!(entity instanceof EntityInsentient)) {
                continue;
            }

            EntityInsentient entityInsentient = (EntityInsentient) entity;

            Location spot = new Location(this.spawnerLocation.getWorld(), x, y, z);
            if (!canSpawn(entityInsentient, spot, canSpawnOn)) {
                continue;
            }

            entityInsentient.prepare(damageScaler, null);

            LivingEntity craftEntity = (LivingEntity) entity.getBukkitEntity();

            if (spawned != null && !spawned.onSpawn(craftEntity)) {
                return null;
            }

            if (particleType != null) {
                float xx = (float) (0 + (Math.random() * 1));
                float yy = (float) (0 + (Math.random() * 2));
                float zz = (float) (0 + (Math.random() * 1));

                CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.getParticle(particleType),
                        spot, 5, xx, yy, zz, 0);
            }

            world.addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);

            spot.setYaw(random.nextFloat() * 360.0F);
            craftEntity.teleport(spot);

            return craftEntity;
        }

        return null;
    }

    private boolean canSpawn(EntityInsentient entityInsentient, Location location, Set<XMaterial> canSpawnOn) {
        if (!entityInsentient.canSpawn()) {
            return false;
        }

        Optional<XMaterial> spawnedIn = CompatibleMaterial.getMaterial(location.getBlock().getType());
        Optional<XMaterial> spawnedOn = CompatibleMaterial.getMaterial(location.getBlock().getRelative(BlockFace.DOWN).getType());

        if (!spawnedIn.isPresent() || !spawnedOn.isPresent()) {
            return false;
        }

        if (!CompatibleMaterial.isAir(spawnedIn.get()) &&
                spawnedIn.get() != XMaterial.WATER &&
                !spawnedIn.get().name().contains("PRESSURE") &&
                !spawnedIn.get().name().contains("SLAB")) {
            return false;
        }

        for (XMaterial material : canSpawnOn) {
            if (material == null) continue;

            if (spawnedOn.get() == material || CompatibleMaterial.isAir(material)) {
                return true;
            }
        }

        return false;
    }
}
