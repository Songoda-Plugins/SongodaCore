package com.songoda.core.nms.v1_17_R1.world.spawner;

import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.level.MobSpawnerAbstract;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_17_R1.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
import java.util.Random;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private final CreatureSpawner bukkitSpawner;
    private final MobSpawnerAbstract spawner;

    private static final Random spawnerFieldP = new Random();   // Field p in MobSpawnerAbstract is private - We use one random for *all* our spawners (should be fine, right?)

    public BBaseSpawnerImpl(CreatureSpawner bukkitSpawner, MobSpawnerAbstract spawner) {
        this.bukkitSpawner = bukkitSpawner;
        this.spawner = spawner;
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#c(World, BlockPosition)}.
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public boolean isNearPlayer() {
        BlockPosition bPos = getBlockPosition();

        return getWorld().isPlayerNearby((double) bPos.getX() + 0.5D, (double) bPos.getY() + 0.5D, (double) bPos.getZ() + 0.5D, this.spawner.n);
    }

    /**
     * This method is based on {@link MobSpawnerAbstract#a(WorldServer, BlockPosition)}.
     */
    @Override
    public void tick() {
        WorldServer world = getWorld();
        BlockPosition bPos = getBlockPosition();

        if (this.spawner.d == -1) {
            this.delay(world, bPos);
        }

        if (this.spawner.d > 0) {
            --this.spawner.d;
            return;
        }

        boolean flag = false;
        int i = 0;

        while (true) {
            if (i >= this.spawner.k) {
                if (flag) {
                    this.delay(world, bPos);
                }

                break;
            }

            NBTTagCompound nbttagcompound = this.spawner.f.getEntity();
            Optional<EntityTypes<?>> optional = EntityTypes.a(nbttagcompound);
            if (optional.isEmpty()) {
                this.delay(world, bPos);
                return;
            }

            NBTTagList nbttaglist = nbttagcompound.getList("Pos", 6);
            int j = nbttaglist.size();
            double d0 = j >= 1 ? nbttaglist.h(0) : (double) bPos.getX() + (world.w.nextDouble() - world.w.nextDouble()) * (double) this.spawner.o + 0.5D;
            double d1 = j >= 2 ? nbttaglist.h(1) : (double) (bPos.getY() + world.w.nextInt(3) - 1);
            double d2 = j >= 3 ? nbttaglist.h(2) : (double) bPos.getZ() + (world.w.nextDouble() - world.w.nextDouble()) * (double) this.spawner.o + 0.5D;
            if (world.b(optional.get().a(d0, d1, d2)) &&
                    EntityPositionTypes.a((EntityTypes<?>) optional.get(), world, EnumMobSpawn.c, new BlockPosition(d0, d1, d2), world.getRandom())) {
                label107:
                {
                    Entity entity = EntityTypes.a(nbttagcompound, world, (entity1) -> {
                        entity1.setPositionRotation(d0, d1, d2, entity1.getYRot(), entity1.getXRot());
                        return entity1;
                    });
                    if (entity == null) {
                        this.delay(world, bPos);
                        return;
                    }

                    int k = world.a(entity.getClass(), (new AxisAlignedBB(bPos.getX(), bPos.getY(), bPos.getZ(), bPos.getX() + 1, bPos.getY() + 1, bPos.getZ() + 1)).g(this.spawner.o)).size();
                    if (k >= this.spawner.m) {
                        this.delay(world, bPos);
                        return;
                    }

                    entity.setPositionRotation(entity.locX(), entity.locY(), entity.locZ(), world.w.nextFloat() * 360.0F, 0.0F);
                    if (entity instanceof EntityInsentient entityinsentient) {
                        if (!entityinsentient.a(world, EnumMobSpawn.c) || !entityinsentient.a(world)) {
                            break label107;
                        }

                        if (this.spawner.f.getEntity().e() == 1 && this.spawner.f.getEntity().hasKeyOfType("id", 8)) {
                            ((EntityInsentient) entity).prepare(world, world.getDamageScaler(entity.getChunkCoordinates()), EnumMobSpawn.c, null, null);
                        }

                        if (entityinsentient.t.spigotConfig.nerfSpawnerMobs) {
                            entityinsentient.aware = false;
                        }
                    }

                    if (CraftEventFactory.callSpawnerSpawnEvent(entity, bPos).isCancelled()) {
                        Entity vehicle = entity.getVehicle();
                        if (vehicle != null) {
                            vehicle.die();
                        }

                        for (Entity passenger : entity.getAllPassengers()) {
                            passenger.die();
                        }
                    } else {
                        if (!world.addAllEntitiesSafely(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                            this.delay(world, bPos);
                            return;
                        }

                        world.triggerEffect(2004, bPos, 0);
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
     * This method is based on {@link MobSpawnerAbstract#d(World, BlockPosition)}.
     */
    @SuppressWarnings("JavadocReference")
    private void delay(WorldServer world, BlockPosition bPos) {
        if (this.spawner.j <= this.spawner.i) {
            this.spawner.d = this.spawner.i;
        } else {
            this.spawner.d = this.spawner.i + spawnerFieldP.nextInt(this.spawner.j - this.spawner.i);
        }

        this.spawner.e.b(spawnerFieldP)
                .ifPresent((mobSpawnerData) -> this.spawner.setSpawnData(world, bPos, mobSpawnerData));
        this.spawner.a(world, getBlockPosition(), 1);
    }

    private WorldServer getWorld() {
        return ((CraftWorld) this.bukkitSpawner.getWorld()).getHandle();
    }

    private BlockPosition getBlockPosition() {
        return ((CraftCreatureSpawner) this.bukkitSpawner).getPosition();
    }
}
