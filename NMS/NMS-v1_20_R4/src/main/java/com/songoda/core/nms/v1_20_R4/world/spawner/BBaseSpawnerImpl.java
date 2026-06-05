package com.songoda.core.nms.v1_20_R4.world.spawner;

import com.songoda.core.nms.world.BBaseSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_20_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R4.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_20_R4.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

public class BBaseSpawnerImpl implements BBaseSpawner {
    private final CreatureSpawner bukkitSpawner;
    private final BaseSpawner spawner;

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
                (double) bPos.getX() + 0.5,
                (double) bPos.getY() + 0.5,
                (double) bPos.getZ() + 0.5,
                this.spawner.requiredPlayerRange
        );
    }

    /**
     * This method is based on {@link BaseSpawner#serverTick(ServerLevel, BlockPos)}.
     */
    @Override
    public void tick() throws InvocationTargetException, IllegalAccessException {
        ServerLevel worldServer = getWorld();
        BlockPos blockposition = getBlockPosition();

        if (this.spawner.spawnDelay == -1) {
            this.delay(worldServer, blockposition);
        }

        if (this.spawner.spawnDelay > 0) {
            --this.spawner.spawnDelay;
        } else {
            boolean flag = false;
            RandomSource randomsource = worldServer.getRandom();
            SpawnData mobSpawnerData = this.getOrCreateNextSpawnData(randomsource);
            int i = 0;

            while (true) {
                if (i >= this.spawner.spawnCount) {
                    if (flag) {
                        this.delay(worldServer, blockposition);
                    }
                    break;
                }

                CompoundTag nbtTagCompound = mobSpawnerData.getEntityToSpawn();
                Optional<EntityType<?>> optional = EntityType.by(nbtTagCompound);
                if (optional.isEmpty()) {
                    this.delay(worldServer, blockposition);
                    return;
                }

                ListTag nbtTagList = nbtTagCompound.getList("Pos", 6);
                int j = nbtTagList.size();
                double d0 = j >= 1 ? nbtTagList.getDouble(0) : (double) blockposition.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) this.spawner.spawnRange + 0.5;
                double d1 = j >= 2 ? nbtTagList.getDouble(1) : (double) (blockposition.getY() + randomsource.nextInt(3) - 1);
                double d2 = j >= 3 ? nbtTagList.getDouble(2) : (double) blockposition.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) this.spawner.spawnRange + 0.5;
                if (worldServer.noCollision(optional.get().getSpawnAABB(d0, d1, d2))) {
                    label119:
                    {
                        BlockPos blockposition1 = BlockPos.containing(d0, d1, d2);
                        if (mobSpawnerData.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && worldServer.getDifficulty() == Difficulty.PEACEFUL) {
                                break label119;
                            }

                            SpawnData.CustomSpawnRules mobSpawnerData2 = mobSpawnerData.getCustomSpawnRules().get();
                            if (!mobSpawnerData2.isValidPosition(blockposition1, worldServer)) {
                                break label119;
                            }
                        } else if (!SpawnPlacements.checkSpawnRules((EntityType<?>) optional.get(), worldServer, MobSpawnType.SPAWNER, blockposition1, worldServer.getRandom())) {
                            break label119;
                        }

                        Entity entity = EntityType.loadEntityRecursive(nbtTagCompound, worldServer, (entity1) -> {
                            entity1.moveTo(d0, d1, d2, entity1.getYRot(), entity1.getXRot());
                            return entity1;
                        });
                        if (entity == null) {
                            this.delay(worldServer, blockposition);
                            return;
                        }

                        int k = worldServer.getEntities(EntityTypeTest.forExactClass(entity.getClass()), (new AABB(blockposition.getX(), blockposition.getY(), blockposition.getZ(), blockposition.getX() + 1, blockposition.getY() + 1, blockposition.getZ() + 1)).inflate((double) this.spawner.spawnRange), EntitySelector.NO_SPECTATORS).size();
                        if (k >= this.spawner.maxNearbyEntities) {
                            this.delay(worldServer, blockposition);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), randomsource.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob entityInsentient) {
                            if (mobSpawnerData.getCustomSpawnRules().isEmpty() && !entityInsentient.checkSpawnRules(worldServer, MobSpawnType.SPAWNER) || !entityInsentient.checkSpawnObstruction(worldServer)) {
                                break label119;
                            }

                            boolean flag1 = mobSpawnerData.getEntityToSpawn().size() == 1 && mobSpawnerData.getEntityToSpawn().contains("id", 8);
                            if (flag1) {
                                ((Mob) entity).finalizeSpawn(worldServer, worldServer.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null);
                            }

                            Optional<EquipmentTable> optional1 = mobSpawnerData.getEquipment();
                            Objects.requireNonNull(entityInsentient);
                            Objects.requireNonNull(entityInsentient);
                            optional1.ifPresent(entityInsentient::equip);
                            if (entityInsentient.level().spigotConfig.nerfSpawnerMobs) {
                                entityInsentient.aware = false;
                            }
                        }

                        if (!CraftEventFactory.callSpawnerSpawnEvent(entity, blockposition).isCancelled()) {
                            if (!worldServer.tryAddFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                                this.delay(worldServer, blockposition);
                                return;
                            }

                            worldServer.levelEvent(2004, blockposition, 0);
                            worldServer.gameEvent(entity, GameEvent.ENTITY_PLACE, blockposition1);
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
        RandomSource randomsource = world.random;
        if (this.spawner.maxSpawnDelay <= this.spawner.minSpawnDelay) {
            this.spawner.spawnDelay = this.spawner.minSpawnDelay;
        } else {
            this.spawner.spawnDelay = this.spawner.minSpawnDelay + randomsource.nextInt(this.spawner.maxSpawnDelay - this.spawner.minSpawnDelay);
        }

        this.spawner.spawnPotentials.getRandom(randomsource).ifPresent((entry) -> this.spawner.nextSpawnData = entry.data());
        this.spawner.broadcastEvent(world, bPos, 1);
    }

    /**
     * This method is based on {@link BaseSpawner#getOrCreateNextSpawnData(Level, RandomSource, BlockPos)}.
     */
    @SuppressWarnings("JavadocReference")
    private SpawnData getOrCreateNextSpawnData(RandomSource randomsource) {
        if (this.spawner.nextSpawnData == null) {
            this.spawner.nextSpawnData = this.spawner.spawnPotentials.getRandom(randomsource).map(WeightedEntry.Wrapper::data).orElseGet(SpawnData::new);
        }
        return this.spawner.nextSpawnData;
    }

    private ServerLevel getWorld() {
        return ((CraftWorld) this.bukkitSpawner.getWorld()).getHandle();
    }

    private BlockPos getBlockPosition() {
        return ((CraftCreatureSpawner) this.bukkitSpawner).getPosition();
    }
}
