package com.songoda.core.nms.v1_18_R2.world.spawner;

import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.phys.AABB;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
import java.util.Random;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private final CreatureSpawner bukkitSpawner;
    private final BaseSpawner spawner;

    private static final Random spawnerRandom = new Random();   // Field random in BaseSpawner is private - We use one random for *all* our spawners (should be fine, right?)

    public BBaseSpawnerImpl(CreatureSpawner bukkitSpawner, BaseSpawner spawner) {
        this.bukkitSpawner = bukkitSpawner;
        this.spawner = spawner;
    }

    /**
     * This method is based on {@link BaseSpawner#isNearPlayer(Level, BlockPos)}.
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public boolean isNearPlayer() {
        BlockPos bPos = getBlockPosition();

        return getWorld().hasNearbyAlivePlayer(
                (double) bPos.getX() + 0.5D,
                (double) bPos.getY() + 0.5D,
                (double) bPos.getZ() + 0.5D,
                this.spawner.requiredPlayerRange
        );
    }

    /**
     * This method is based on {@link BaseSpawner#serverTick(ServerLevel, BlockPos)}.
     */
    @Override
    public void tick() {
        ServerLevel world = getWorld();
        BlockPos bPos = getBlockPosition();

        if (this.spawner.spawnDelay == -1) {
            delay(world, bPos);
        }

        if (this.spawner.spawnDelay > 0) {
            --this.spawner.spawnDelay;
        } else {
            boolean flag = false;
            int i = 0;

            while (true) {
                if (i >= this.spawner.spawnCount) {
                    if (flag) {
                        delay(world, bPos);
                    }

                    break;
                }

                CompoundTag nbtTagCompound = this.spawner.nextSpawnData.getEntityToSpawn();
                Optional<EntityType<?>> optional = EntityType.by(nbtTagCompound);
                if (optional.isEmpty()) {
                    delay(world, bPos);
                    return;
                }

                ListTag nbtTagList = nbtTagCompound.getList("Pos", 6);
                int j = nbtTagList.size();
                double d0 = j >= 1 ? nbtTagList.getDouble(0) : (double) bPos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double) this.spawner.spawnRange + 0.5D;
                double d1 = j >= 2 ? nbtTagList.getDouble(1) : (double) (bPos.getY() + world.random.nextInt(3) - 1);
                double d2 = j >= 3 ? nbtTagList.getDouble(2) : (double) bPos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double) this.spawner.spawnRange + 0.5D;
                if (world.noCollision(optional.get().getAABB(d0, d1, d2))) {
                    label128:
                    {
                        BlockPos blockPosition1 = new BlockPos(d0, d1, d2);
                        if (this.spawner.nextSpawnData.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && world.getDifficulty() == Difficulty.PEACEFUL) {
                                break label128;
                            }

                            SpawnData.CustomSpawnRules mobSpawnerDataA = this.spawner.nextSpawnData.getCustomSpawnRules().get();
                            if (!mobSpawnerDataA.blockLightLimit().isValueInRange(world.getBrightness(LightLayer.BLOCK, blockPosition1)) ||
                                    !mobSpawnerDataA.skyLightLimit().isValueInRange(world.getBrightness(LightLayer.SKY, blockPosition1))) {
                                break label128;
                            }
                        } else if (!SpawnPlacements.checkSpawnRules(optional.get(), world, MobSpawnType.SPAWNER, blockPosition1, world.getRandom())) {
                            break label128;
                        }

                        Entity entity = EntityType.loadEntityRecursive(nbtTagCompound, world, (entity1) -> {
                            entity1.moveTo(d0, d1, d2, entity1.getYRot(), entity1.getXRot());
                            return entity1;
                        });
                        if (entity == null) {
                            delay(world, bPos);
                            return;
                        }

                        int k = world.getEntitiesOfClass(entity.getClass(), (new AABB(bPos.getX(), bPos.getY(), bPos.getZ(), bPos.getX() + 1, bPos.getY() + 1, bPos.getZ() + 1)).inflate(this.spawner.spawnRange)).size();
                        if (k >= this.spawner.maxNearbyEntities) {
                            delay(world, bPos);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob entityInsentient) {
                            if (this.spawner.nextSpawnData.getCustomSpawnRules().isEmpty() && !entityInsentient.checkSpawnRules(world, MobSpawnType.SPAWNER) || !entityInsentient.checkSpawnObstruction(world)) {
                                break label128;
                            }

                            if (this.spawner.nextSpawnData.getEntityToSpawn().size() == 1 && this.spawner.nextSpawnData.getEntityToSpawn().contains("id", 8)) {
                                ((Mob) entity).finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
                            }

                            if (entityInsentient.level.spigotConfig.nerfSpawnerMobs) {
                                entityInsentient.aware = false;
                            }
                        }

                        if (CraftEventFactory.callSpawnerSpawnEvent(entity, bPos).isCancelled()) {
                            Entity vehicle = entity.getVehicle();
                            if (vehicle != null) {
                                vehicle.discard();
                            }

                            for (Entity passenger : entity.getIndirectPassengers()) {
                                passenger.discard();
                            }
                        } else {
                            if (!world.tryAddFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                                delay(world, bPos);
                                return;
                            }

                            world.levelEvent(2004, bPos, 0);
                            if (entity instanceof Mob) {
                                ((Mob) entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }
                }

                ++i;
            }
        }
    }

    /**
     * This method is based on {@link BaseSpawner#delay(Level, BlockPos)}.
     */
    @SuppressWarnings("JavadocReference")
    private void delay(ServerLevel world, BlockPos bPos) {
        if (this.spawner.maxSpawnDelay <= this.spawner.minSpawnDelay) {
            this.spawner.spawnDelay = this.spawner.minSpawnDelay;
        } else {
            this.spawner.spawnDelay = this.spawner.minSpawnDelay + spawnerRandom.nextInt(this.spawner.maxSpawnDelay - this.spawner.minSpawnDelay);
        }

        this.spawner.spawnPotentials.getRandom(spawnerRandom).ifPresent((weightedEntryB) -> this.spawner.setNextSpawnData(world, bPos, weightedEntryB.getData()));
        this.spawner.broadcastEvent(world, bPos, 1);
    }

    private ServerLevel getWorld() {
        return ((CraftWorld) this.bukkitSpawner.getWorld()).getHandle();
    }

    private BlockPos getBlockPosition() {
        return ((CraftCreatureSpawner) this.bukkitSpawner).getPosition();
    }
}
