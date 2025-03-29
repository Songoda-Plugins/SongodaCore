package com.songoda.core.world;

import com.songoda.core.hooks.EntityStackerManager;
import com.songoda.core.nms.Nms;
import com.songoda.core.nms.world.SpawnedEntity;
import com.songoda.core.utils.EntityUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SSpawner {
    protected final com.songoda.core.nms.world.SSpawner sSpawner;
    protected final Location location;

    public SSpawner(Location location) {
        this.location = location;
        this.sSpawner = Nms.getImplementations().getWorld().getSpawner(location);
    }

    public SSpawner(CreatureSpawner spawner) {
        this(spawner.getLocation());
    }

    public int spawn(int amountToSpawn, EntityType... types) {
        return spawn(amountToSpawn, "EXPLOSION_NORMAL", null, null, types);
    }

    /**
     * Spawn the spawner.
     * <p>
     * If you want support for stackers you will need to load them
     * on your plugins enable.
     *
     * @return amount of entities spawned
     */
    public int spawn(int amountToSpawn, String particle, Set<XMaterial> canSpawnOn, SpawnedEntity spawned, EntityType... types) {
        if (this.location.getWorld() == null) {
            return 0;
        }

        if (canSpawnOn == null) {
            canSpawnOn = new HashSet<>();
        }

        if (canSpawnOn.isEmpty()) {
            canSpawnOn.addAll(EntityUtils.getSpawnBlocks(types[0]));
        }

        boolean useStackPlugin = EntityStackerManager.isEnabled();

        int spawnCountUsed = useStackPlugin ? 1 : amountToSpawn;

        int amountSpawned = 0;
        while (spawnCountUsed-- > 0) {
            EntityType type = types[ThreadLocalRandom.current().nextInt(types.length)];
            LivingEntity entity = this.sSpawner.spawnEntity(type, particle, spawned, canSpawnOn);

            if (entity != null) {
                // If this entity is indeed stackable then spawn a single stack with the desired stack size.
                if (useStackPlugin && amountToSpawn >= EntityStackerManager.getMinStackSize(type)) {
                    EntityStackerManager.add(entity, amountToSpawn);
                    amountSpawned = amountToSpawn;

                    break;
                }

                amountSpawned++;
            }
        }

        return amountSpawned;
    }

    public Location getLocation() {
        return this.location;
    }
}
