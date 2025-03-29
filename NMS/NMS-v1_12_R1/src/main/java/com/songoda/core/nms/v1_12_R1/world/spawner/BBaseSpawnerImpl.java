package com.songoda.core.nms.v1_12_R1.world.spawner;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.server.v1_12_R1.AxisAlignedBB;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.ChunkRegionLoader;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.MobSpawnerAbstract;
import net.minecraft.server.v1_12_R1.MobSpawnerData;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

        return spawner.a().isPlayerNearby(
                (double) blockposition.getX() + 0.5D,
                (double) blockposition.getY() + 0.5D,
                (double) blockposition.getZ() + 0.5D,
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
            spawner.a().addParticle(EnumParticle.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            spawner.a().addParticle(EnumParticle.FLAME, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            if (spawner.spawnDelay > 0) {
                --spawner.spawnDelay;
            }

            double spawnerD = (double) ReflectionUtils.getFieldValue(spawner, "d");

            ReflectionUtils.setFieldValue(spawner, "e", spawnerD);
            ReflectionUtils.setFieldValue(spawner, "d", (spawnerD + (double) (1000F / ((float) spawner.spawnDelay + 200F))) % 360D);
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

            net.minecraft.server.v1_12_R1.World world = spawner.a();

            int j = nbttaglist.size();
            double d3 = j >= 1 ? nbttaglist.f(0) : (double) blockposition.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;
            double d4 = j >= 2 ? nbttaglist.f(1) : (double) (blockposition.getY() + world.random.nextInt(3) - 1);
            double d5 = j >= 3 ? nbttaglist.f(2) : (double) blockposition.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double) spawner.spawnRange + .5D;

            Entity entity = ChunkRegionLoader.a(nbttagcompound, world, d3, d4, d5, false);

            if (entity == null) {
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

            if (entityinsentient == null || entityinsentient.P() && entityinsentient.canSpawn()) {
                if (spawnData.b().d() == 1 && spawnData.b().hasKeyOfType("id", 8) && entity instanceof EntityInsentient) {
                    ((EntityInsentient) entity).prepare(world.D(new BlockPosition(entity)), null);
                }

                if (entity.world.spigotConfig.nerfSpawnerMobs) {
                    entity.fromMobSpawner = true;
                }

                if (!CraftEventFactory.callSpawnerSpawnEvent(entity, blockposition).isCancelled()) {
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
