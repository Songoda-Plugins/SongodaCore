package com.songoda.core.nms.v1_14_R1.world.spawner;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.server.v1_14_R1.AxisAlignedBB;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityInsentient;
import net.minecraft.server.v1_14_R1.EntityPositionTypes;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumMobSpawn;
import net.minecraft.server.v1_14_R1.MobSpawnerAbstract;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.Particles;
import net.minecraft.server.v1_14_R1.WeightedRandom;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Iterator;
import java.util.Optional;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private final MobSpawnerAbstract spawner;

    public BBaseSpawnerImpl(MobSpawnerAbstract spawner) {
        this.spawner = spawner;
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#h()}.
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public boolean isNearPlayer() {
        BlockPosition blockposition = spawner.b();

        return spawner.a().isPlayerNearby(
                (double) blockposition.getX() + .5D,
                (double) blockposition.getY() + .5D,
                (double) blockposition.getZ() + .5D,
                spawner.requiredPlayerRange);
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#c()}.
     */
    @Override
    public void tick() throws NoSuchFieldException, IllegalAccessException {
        net.minecraft.server.v1_14_R1.World world = spawner.a();
        BlockPosition blockposition = spawner.b();

        if (world.isClientSide) {
            double d0 = (float) blockposition.getX() + world.random.nextFloat();
            double d1 = (float) blockposition.getY() + world.random.nextFloat();
            double d2 = (float) blockposition.getZ() + world.random.nextFloat();

            world.addParticle(Particles.SMOKE, d0, d1, d2, 0D, 0D, 0D);
            world.addParticle(Particles.FLAME, d0, d1, d2, 0D, 0D, 0D);

            if (spawner.spawnDelay > 0) {
                --spawner.spawnDelay;
            }

            double spawnerE = (double) ReflectionUtils.getFieldValue(spawner, "e");
            ReflectionUtils.setFieldValue(spawner, "f", spawnerE);
            ReflectionUtils.setFieldValue(spawner, "e", (spawnerE + (double) (1000F / ((float) spawner.spawnDelay + 200F))) % 360D);
            return;
        }

        if (spawner.spawnDelay == -1) {
            delay(spawner);
        }

        if (spawner.spawnDelay > 0) {
            --spawner.spawnDelay;
            return;
        }

        boolean flag = false;
        int i = 0;

        while (true) {
            if (i >= spawner.spawnCount) {
                if (flag) {
                    delay(spawner);
                }

                break;
            }

            NBTTagCompound nbttagcompound = spawner.spawnData.getEntity();
            Optional<EntityTypes<?>> optional = EntityTypes.a(nbttagcompound);
            if (!optional.isPresent()) {
                delay(spawner);
                return;
            }

            NBTTagList nbttaglist = nbttagcompound.getList("Pos", 6);
            int j = nbttaglist.size();
            double d3 = j >= 1 ? nbttaglist.h(0) : (double) blockposition.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;
            double d4 = j >= 2 ? nbttaglist.h(1) : (double) (blockposition.getY() + world.random.nextInt(3) - 1);
            double d5 = j >= 3 ? nbttaglist.h(2) : (double) blockposition.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;
            if (world.c(optional.get().a(d3, d4, d5)) && EntityPositionTypes.a(optional.get(), world.getMinecraftWorld(), EnumMobSpawn.SPAWNER, new BlockPosition(d3, d4, d5), world.getRandom())) {
                label112:
                {
                    Entity entity = EntityTypes.a(nbttagcompound, world, (entity1) -> {
                        entity1.setPositionRotation(d3, d4, d5, entity1.yaw, entity1.pitch);
                        return entity1;
                    });
                    if (entity == null) {
                        delay(spawner);
                        return;
                    }

                    int k = world.a(entity.getClass(), (new AxisAlignedBB(
                            blockposition.getX(),
                            blockposition.getY(),
                            blockposition.getZ(),
                            blockposition.getX() + 1,
                            blockposition.getY() + 1,
                            blockposition.getZ() + 1))
                            .g(spawner.spawnRange)).size();

                    if (k >= spawner.maxNearbyEntities) {
                        delay(spawner);
                        return;
                    }

                    entity.setPositionRotation(entity.locX, entity.locY, entity.locZ, world.random.nextFloat() * 360F, 0F);
                    if (entity instanceof EntityInsentient) {
                        EntityInsentient entityinsentient = (EntityInsentient) entity;
                        if (!entityinsentient.a(world, EnumMobSpawn.SPAWNER) || !entityinsentient.a(world)) {
                            break label112;
                        }

                        if (spawner.spawnData.getEntity().d() == 1 && spawner.spawnData.getEntity().hasKeyOfType("id", 8)) {
                            ((EntityInsentient) entity).prepare(world, world.getDamageScaler(new BlockPosition(entity)), EnumMobSpawn.SPAWNER, null, null);
                        }
                    }

                    if (entity.world.spigotConfig.nerfSpawnerMobs) {
                        entity.fromMobSpawner = true;
                    }

                    if (CraftEventFactory.callSpawnerSpawnEvent(entity, blockposition).isCancelled()) {
                        Entity vehicle = entity.getVehicle();
                        if (vehicle != null) {
                            vehicle.dead = true;
                        }

                        Entity passenger;
                        for (Iterator<Entity> var19 = entity.getAllPassengers().iterator(); var19.hasNext(); passenger.dead = true) {
                            passenger = var19.next();
                        }
                    } else {
                        addWithPassengers(spawner, entity);
                        world.triggerEffect(2004, blockposition, 0);

                        if (entity instanceof EntityInsentient) {
                            ((EntityInsentient) entity).doSpawnEffect();
                        }

                        flag = true;
                    }
                }
            }

            ++i;
        }
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#i()}.
     */
    @SuppressWarnings("JavadocReference")
    private void delay(MobSpawnerAbstract spawner) {
        if (spawner.maxSpawnDelay <= spawner.minSpawnDelay) {
            spawner.spawnDelay = spawner.minSpawnDelay;
        } else {
            int i = spawner.maxSpawnDelay - spawner.minSpawnDelay;
            spawner.spawnDelay = spawner.minSpawnDelay + spawner.a().random.nextInt(i);
        }

        if (!spawner.mobs.isEmpty()) {
            spawner.setSpawnData(WeightedRandom.a(spawner.a().random, spawner.mobs));
        }

        spawner.a(1);
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#a(Entity)}.
     */
    @SuppressWarnings("JavadocReference")
    static void addWithPassengers(MobSpawnerAbstract spawner, Entity entity) {
        if (spawner.a().addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
            for (Entity value : entity.getPassengers()) {
                addWithPassengers(spawner, value);
            }
        }
    }
}
