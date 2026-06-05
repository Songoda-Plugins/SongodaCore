package com.songoda.core.hooks.stackers;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.enums.SpawnCause;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.songoda.core.SongodaPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class WildStacker extends Stacker {
    private final WildStackerPlugin plugin;

    public WildStacker() {
        this.plugin = WildStackerPlugin.getPlugin();
    }

    @Override
    public String getName() {
        return "WildStacker";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsItemStacking() {
        return true;
    }

    @Override
    public boolean supportsEntityStacking() {
        return true;
    }

    @Override
    public void setItemAmount(Item item, int amount) {
        WildStackerAPI.getStackedItem(item).setStackAmount(amount, true);
    }

    @Override
    public int getItemAmount(Item item) {
        return WildStackerAPI.getItemAmount(item);
    }

    @Override
    public boolean isStacked(LivingEntity entity) {
        return WildStackerAPI.getEntityAmount(entity) != 0;
    }

    @Override
    public int getSize(LivingEntity entity) {
        return WildStackerAPI.getEntityAmount(entity);
    }

    @Override
    public void remove(LivingEntity entity, int amount) {
        StackedEntity stackedEntity = WildStackerAPI.getStackedEntity(entity);
        stackedEntity.setStackAmount(stackedEntity.getStackAmount() - amount, true);
    }

    @Override
    public void add(LivingEntity entity, int amount) {
        StackedEntity stackedEntity = WildStackerAPI.getStackedEntity(entity);
        stackedEntity.setStackAmount(stackedEntity.getStackAmount() + amount, true);
    }

    @Override
    public int getMinStackSize(EntityType type) {
        return plugin.getSettings().minimumRequiredEntities.get(type, SpawnCause.DEFAULT);
    }
}
