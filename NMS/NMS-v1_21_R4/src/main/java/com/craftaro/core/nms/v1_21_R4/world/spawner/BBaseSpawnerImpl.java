package com.craftaro.core.nms.v1_21_R4.world.spawner;

import com.craftaro.core.nms.world.BBaseSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_21_R4.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;

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
                (double) bPos.getX() + (double) 0.5F,
                (double) bPos.getY() + (double) 0.5F,
                (double) bPos.getZ() + (double) 0.5F,
                this.spawner.requiredPlayerRange
        );
    }

    /**
     * This method is based on {@link BaseSpawner#serverTick(ServerLevel, BlockPos)}.
     */
    @Override
    public void tick() {
        ServerLevel worldserver = getWorld();
        BlockPos blockposition = getBlockPosition();

        if (this.spawner.spawnDelay == -1) {
            this.delay(worldserver, blockposition);
        }

        if (this.spawner.spawnDelay > 0) {
            --this.spawner.spawnDelay;
        } else {
            boolean flag = false;
            RandomSource randomsource = worldserver.getRandom();
            SpawnData mobSpawnerData = this.getOrCreateNextSpawnData(randomsource);

            for (int i = 0; i < this.spawner.spawnCount; ++i) {
                CompoundTag nbttagcompound = mobSpawnerData.getEntityToSpawn();
                Optional<EntityType<?>> optional = EntityType.by(nbttagcompound);
                if (optional.isEmpty()) {
                    this.delay(worldserver, blockposition);
                    return;
                }

                Vec3 vec3d = nbttagcompound.read("Pos", Vec3.CODEC).orElseGet(() -> new Vec3((double) blockposition.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) this.spawner.spawnRange + (double) 0.5F, blockposition.getY() + randomsource.nextInt(3) - 1, (double) blockposition.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double) this.spawner.spawnRange + (double) 0.5F));
                if (worldserver.noCollision(optional.get().getSpawnAABB(vec3d.x, vec3d.y, vec3d.z))) {
                    BlockPos blockposition1 = BlockPos.containing(vec3d);
                    if (mobSpawnerData.getCustomSpawnRules().isPresent()) {
                        if (!optional.get().getCategory().isFriendly() && worldserver.getDifficulty() == Difficulty.PEACEFUL) {
                            continue;
                        }

                        SpawnData.CustomSpawnRules mobSpawnerDataA = mobSpawnerData.getCustomSpawnRules().get();
                        if (!mobSpawnerDataA.isValidPosition(blockposition1, worldserver)) {
                            continue;
                        }
                    } else if (!SpawnPlacements.checkSpawnRules((EntityType<?>) optional.get(), worldserver, EntitySpawnReason.SPAWNER, blockposition1, worldserver.getRandom())) {
                        continue;
                    }

                    Entity entity = EntityType.loadEntityRecursive(nbttagcompound, worldserver, EntitySpawnReason.SPAWNER, (entity1) -> {
                        entity1.snapTo(vec3d.x, vec3d.y, vec3d.z, entity1.getYRot(), entity1.getXRot());
                        return entity1;
                    });
                    if (entity == null) {
                        this.delay(worldserver, blockposition);
                        return;
                    }

                    int j = worldserver.getEntities(EntityTypeTest.forExactClass(entity.getClass()), (new AABB(blockposition.getX(), blockposition.getY(), blockposition.getZ(), blockposition.getX() + 1, blockposition.getY() + 1, blockposition.getZ() + 1)).inflate(this.spawner.spawnRange), EntitySelector.NO_SPECTATORS).size();
                    if (j >= this.spawner.maxNearbyEntities) {
                        this.delay(worldserver, blockposition);
                        return;
                    }

                    entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), randomsource.nextFloat() * 360.0F, 0.0F);
                    if (entity instanceof Mob entityInsentient) {
                        if (mobSpawnerData.getCustomSpawnRules().isEmpty() && !entityInsentient.checkSpawnRules(worldserver, EntitySpawnReason.SPAWNER) || !entityInsentient.checkSpawnObstruction(worldserver)) {
                            continue;
                        }

                        boolean flag1 = mobSpawnerData.getEntityToSpawn().size() == 1 && mobSpawnerData.getEntityToSpawn().getString("id").isPresent();
                        if (flag1) {
                            ((Mob) entity).finalizeSpawn(worldserver, worldserver.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
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
                        if (!worldserver.tryAddFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                            this.delay(worldserver, blockposition);
                            return;
                        }

                        worldserver.levelEvent(2004, blockposition, 0);
                        worldserver.gameEvent(entity, GameEvent.ENTITY_PLACE, blockposition1);
                        if (entity instanceof Mob) {
                            ((Mob) entity).spawnAnim();
                        }

                        flag = true;
                    }
                }
            }

            if (flag) {
                this.delay(worldserver, blockposition);
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

        this.spawner.spawnPotentials.getRandom(randomsource).ifPresent((mobSpawnerData) -> this.spawner.nextSpawnData = mobSpawnerData);
        this.spawner.broadcastEvent(world, bPos, 1);
    }

    /**
     * This method is based on {@link BaseSpawner#getOrCreateNextSpawnData(Level, RandomSource, BlockPos)}.
     */
    @SuppressWarnings("JavadocReference")
    private SpawnData getOrCreateNextSpawnData(RandomSource randomsource) {
        if (this.spawner.nextSpawnData == null) {
            this.spawner.nextSpawnData = this.spawner.spawnPotentials.getRandom(randomsource).orElseGet(SpawnData::new);
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
