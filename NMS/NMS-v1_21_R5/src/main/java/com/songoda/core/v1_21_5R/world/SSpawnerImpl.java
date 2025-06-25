package com.songoda.core.v1_21_5R.world;

import com.cryptomorin.xseries.XMaterial;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SpawnedEntity;
import com.songoda.core.v1_21_5R.util.CompoundTagConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.SpawnData;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
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
        SpawnData data = new SpawnData();
        CompoundTag compound = data.getEntityToSpawn();

        String name = type
                .name()
                .toLowerCase()
                .replace("snowman", "snow_golem")
                .replace("mushroom_cow", "mooshroom");
        compound.putString("id", "minecraft:" + name);

        short spawnRange = 4;
        for (int i = 0; i < 50; ++i) {
            assert this.spawnerLocation.getWorld() != null;
            ServerLevel world = ((CraftWorld) this.spawnerLocation.getWorld()).getHandle();

            RandomSource random = world.getRandom();
            double x = this.spawnerLocation.getX() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;
            double y = this.spawnerLocation.getY() + random.nextInt(3) - 1;
            double z = this.spawnerLocation.getZ() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;

            Optional<Entity> optionalEntity = net.minecraft.world.entity.EntityType.create(CompoundTagConverter.toInput(compound, world), world, EntitySpawnReason.SPAWNER);
            if (optionalEntity.isEmpty()) {
                continue;
            }

            Entity entity = optionalEntity.get();
            entity.setPos(x, y, z);

            BlockPos position = entity.blockPosition();
            DifficultyInstance damageScaler = world.getCurrentDifficultyAt(position);

            if (!(entity instanceof Mob entityInsentient)) {
                continue;
            }

            Location spot = new Location(this.spawnerLocation.getWorld(), x, y, z);

            if (!canSpawn(world, entityInsentient, spot, canSpawnOn)) {
                continue;
            }

            entityInsentient.finalizeSpawn(world, damageScaler, EntitySpawnReason.SPAWNER, null);

            LivingEntity craftEntity = (LivingEntity) entity.getBukkitEntity();

            if (spawned != null && !spawned.onSpawn(craftEntity)) {
                return null;
            }

            if (particleType != null) {
                float xx = (float) (0 + (Math.random() * 1));
                float yy = (float) (0 + (Math.random() * 2));
                float zz = (float) (0 + (Math.random() * 1));

                CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.getParticle(particleType), spot, 5, xx, yy, zz, 0);
            }

            world.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);

            spot.setYaw(random.nextFloat() * 360.0F);
            craftEntity.teleport(spot);

            return craftEntity;
        }

        return null;
    }

    private boolean canSpawn(ServerLevel world, Mob entityInsentient, Location location, Set<XMaterial> canSpawnOn) {
        if (!world.noCollision(entityInsentient, entityInsentient.getBoundingBox())) {
            return false;
        }

        Optional<XMaterial> spawnedIn = CompatibleMaterial.getMaterial(location.getBlock().getType());
        Optional<XMaterial> spawnedOn = CompatibleMaterial.getMaterial(location.getBlock().getRelative(BlockFace.DOWN).getType());

        if (spawnedIn.isEmpty() || spawnedOn.isEmpty()) {
            return false;
        }

        if (!CompatibleMaterial.isAir(spawnedIn.get()) &&
                spawnedIn.get() != XMaterial.WATER &&
                !spawnedIn.get().name().contains("PRESSURE") &&
                !spawnedIn.get().name().contains("SLAB")) {
            return false;
        }

        for (XMaterial material : canSpawnOn) {
            if (material == null) {
                continue;
            }

            if (spawnedOn.get() == material || CompatibleMaterial.isAir(material)) {
                return true;
            }
        }

        return false;
    }
}
