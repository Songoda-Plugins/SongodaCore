package com.songoda.core.nms.v1_13_R2.world.spawner;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkRegionLoader;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.MobSpawnerAbstract;
import net.minecraft.server.v1_13_R2.MobSpawnerData;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.Particles;
import org.bukkit.craftbukkit.v1_13_R2.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private static Method iMethod;

    private final MobSpawnerAbstract spawner;

    static {
        try {
            iMethod = MobSpawnerAbstract.class.getDeclaredMethod("i");
            iMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

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

        return spawner.a().b(
                (double) blockposition.getX() + .5D,
                (double) blockposition.getY() + .5D,
                (double) blockposition.getZ() + .5D,
                spawner.requiredPlayerRange);
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#c()}.
     */
    @Override
    public void tick() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        BlockPosition blockposition = spawner.b();

        if (spawner.a().isClientSide) {
            double d0 = (float) blockposition.getX() + spawner.a().random.nextFloat();
            double d1 = (float) blockposition.getY() + spawner.a().random.nextFloat();
            double d2 = (float) blockposition.getZ() + spawner.a().random.nextFloat();
            spawner.a().addParticle(Particles.M, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            spawner.a().addParticle(Particles.y, d0, d1, d2, 0.0D, 0.0D, 0.0D);
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

        MobSpawnerData spawnData = (MobSpawnerData) ReflectionUtils.getFieldValue(spawner, "spawnData");

        while (true) {
            if (i >= spawner.spawnCount) {
                if (flag) {
                    delay(spawner);
                }

                break;
            }

            NBTTagCompound nbttagcompound = spawnData.b();
            NBTTagList nbttaglist = nbttagcompound.getList("Pos", 6);
            net.minecraft.server.v1_13_R2.World world = spawner.a();
            int j = nbttaglist.size();
            double d3 = j >= 1 ? nbttaglist.k(0) : (double) blockposition.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;
            double d4 = j >= 2 ? nbttaglist.k(1) : (double) (blockposition.getY() + world.random.nextInt(3) - 1);
            double d5 = j >= 3 ? nbttaglist.k(2) : (double) blockposition.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;
            Entity entity = ChunkRegionLoader.a(nbttagcompound, world, d3, d4, d5, false);
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

            EntityInsentient entityinsentient = entity instanceof EntityInsentient ? (EntityInsentient) entity : null;
            entity.setPositionRotation(entity.locX, entity.locY, entity.locZ, world.random.nextFloat() * 360.0F, 0.0F);
            if (entityinsentient == null || entityinsentient.a(world, true) && entityinsentient.canSpawn()) {
                if (spawnData.b().d() == 1 && spawnData.b().hasKeyOfType("id", 8) && entity instanceof EntityInsentient) {
                    ((EntityInsentient) entity).prepare(world.getDamageScaler(new BlockPosition(entity)), null, null);
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
                    ChunkRegionLoader.a(entity, world, CreatureSpawnEvent.SpawnReason.SPAWNER);
                    world.triggerEffect(2004, blockposition, 0);

                    if (entityinsentient != null) {
                        entityinsentient.doSpawnEffect();
                    }

                    flag = true;
                }
            }

            ++i;
        }
    }

    /**
     * This method calls {@link MobSpawnerAbstract#i()} using Reflections.
     */
    @SuppressWarnings("JavadocReference")
    private void delay(MobSpawnerAbstract spawner) throws InvocationTargetException, IllegalAccessException {
        iMethod.invoke(spawner);
    }
}
