package com.songoda.core.hooks.stackers;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.OutdatedHookInterface;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public abstract class Stacker implements OutdatedHookInterface {
    public abstract boolean supportsItemStacking();

    public abstract boolean supportsEntityStacking();

    public abstract void setItemAmount(Item item, int amount);

    public abstract int getItemAmount(Item item);

    public abstract boolean isStacked(LivingEntity entity);

    public abstract int getSize(LivingEntity entity);

    public void removeOne(LivingEntity entity) {
        remove(entity, 1);
    }

    public abstract void remove(LivingEntity entity, int amount);

    public void addOne(LivingEntity entity) {
        add(entity, 1);
    }

    public abstract void add(LivingEntity entity, int amount);

    public abstract int getMinStackSize(EntityType type);
}
