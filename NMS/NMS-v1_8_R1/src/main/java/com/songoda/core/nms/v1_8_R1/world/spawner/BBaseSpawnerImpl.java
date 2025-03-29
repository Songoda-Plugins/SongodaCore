package com.songoda.core.nms.v1_8_R1.world.spawner;

import com.songoda.core.nms.ReflectionUtils;
import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.server.v1_8_R1.AxisAlignedBB;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.Entity;
import net.minecraft.server.v1_8_R1.EntityInsentient;
import net.minecraft.server.v1_8_R1.EntityTypes;
import net.minecraft.server.v1_8_R1.EnumParticle;
import net.minecraft.server.v1_8_R1.MobSpawnerAbstract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private static Method aEntityBooleanMethod, gMethod, hMethod;

    private final MobSpawnerAbstract spawner;

    static {
        try {
            aEntityBooleanMethod = MobSpawnerAbstract.class.getDeclaredMethod("a", Entity.class, boolean.class);
            aEntityBooleanMethod.setAccessible(true);

            gMethod = MobSpawnerAbstract.class.getDeclaredMethod("g");
            gMethod.setAccessible(true);

            hMethod = MobSpawnerAbstract.class.getDeclaredMethod("h");
            hMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public BBaseSpawnerImpl(MobSpawnerAbstract spawner) {
        this.spawner = spawner;
    }

    /**
     * This method calls {@link MobSpawnerAbstract#g()} using Reflections.
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public boolean isNearPlayer() throws InvocationTargetException, IllegalAccessException {
        return (boolean) gMethod.invoke(spawner);
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#c()}.
     */
    @Override
    public void tick() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        BlockPosition blockposition = spawner.b();

        if (spawner.a().isStatic) {
            double d1 = (float) blockposition.getX() + spawner.a().random.nextFloat();
            double d2 = (float) blockposition.getY() + spawner.a().random.nextFloat();
            double d0 = (float) blockposition.getZ() + spawner.a().random.nextFloat();

            spawner.a().addParticle(EnumParticle.SMOKE_NORMAL, d1, d2, d0, 0D, 0D, 0D);
            spawner.a().addParticle(EnumParticle.FLAME, d1, d2, d0, 0D, 0D, 0D);

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

        int spawnCount = (int) ReflectionUtils.getFieldValue(spawner, "spawnCount");
        int spawnRange = (int) ReflectionUtils.getFieldValue(spawner, "spawnRange");
        int maxNearbyEntities = (int) ReflectionUtils.getFieldValue(spawner, "maxNearbyEntities");
        while (true) {
            if (i >= spawnCount) {
                if (flag) {
                    delay(spawner);
                }

                break;
            }

            Entity entity = EntityTypes.createEntityByName(spawner.getMobName(), spawner.a());
            if (entity == null) {
                return;
            }

            int j = spawner.a()
                    .a(entity.getClass(), (new AxisAlignedBB(blockposition.getX(),
                            blockposition.getY(),
                            blockposition.getZ(),
                            blockposition.getX() + 1,
                            blockposition.getY() + 1,
                            blockposition.getZ() + 1))
                            .grow(spawnRange, spawnRange, spawnRange)).size();

            if (j >= maxNearbyEntities) {
                delay(spawner);
                return;
            }

            double d0 = (double) blockposition.getX() + (spawner.a().random.nextDouble() - spawner.a().random.nextDouble()) * (double) spawnRange + 0.5D;
            double d3 = blockposition.getY() + spawner.a().random.nextInt(3) - 1;
            double d4 = (double) blockposition.getZ() + (spawner.a().random.nextDouble() - spawner.a().random.nextDouble()) * (double) spawnRange + 0.5D;

            EntityInsentient entityinsentient = entity instanceof EntityInsentient ? (EntityInsentient) entity : null;
            entity.setPositionRotation(d0, d3, d4, spawner.a().random.nextFloat() * 360.0F, 0.0F);

            if (entityinsentient == null || entityinsentient.bQ() && entityinsentient.canSpawn()) {
                aEntityBooleanMethod.invoke(spawner, entity, true);
                spawner.a().triggerEffect(2004, blockposition, 0);

                if (entityinsentient != null) {
                    entityinsentient.y();
                }

                flag = true;
            }

            ++i;
        }
    }

    /**
     * This method calls {@link MobSpawnerAbstract#h()} using Reflections.
     */
    @SuppressWarnings("JavadocReference")
    private void delay(MobSpawnerAbstract spawner) throws InvocationTargetException, IllegalAccessException {
        hMethod.invoke(spawner);
    }
}
