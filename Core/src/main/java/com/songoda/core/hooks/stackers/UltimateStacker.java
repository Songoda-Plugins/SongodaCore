package com.songoda.core.hooks.stackers;

import com.songoda.core.SongodaPlugin;
import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.entity.EntityStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class UltimateStacker extends Stacker {
    private final Plugin plugin;

    public UltimateStacker() {
        this.plugin = Bukkit.getPluginManager().getPlugin("UltimateStacker");
    }

    @Override
    public String getName() {
        return "UltimateStacker";
    }

    @Override
    public boolean isEnabled() {
        return this.plugin.isEnabled();
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
        UltimateStackerApi.getStackedItemManager().updateStack(item, amount);
    }

    @Override
    public int getItemAmount(Item item) {
        return UltimateStackerApi.getStackedItemManager().getActualItemAmount(item);
    }

    @Override
    public boolean isStacked(LivingEntity entity) {
        return UltimateStackerApi.getEntityStackManager().isStackedEntity(entity);
    }

    @Override
    public int getSize(LivingEntity entity) {
        return isStacked(entity) ? UltimateStackerApi.getEntityStackManager().getStackedEntity(entity).getAmount() : 0;
    }

    @Override
    public void remove(LivingEntity entity, int amount) {
        if (!isStacked(entity)) return;
        EntityStack stack = UltimateStackerApi.getEntityStackManager().getStackedEntity(entity);
        stack.take(amount);
    }

    @Override
    public void add(LivingEntity entity, int amount) {
        if (!isStacked(entity)) {
            UltimateStackerApi.getEntityStackManager().createStackedEntity(entity, amount+1);
        } else {
            EntityStack stack = UltimateStackerApi.getEntityStackManager().getStackedEntity(entity);
            stack.add(amount);
        }
    }

    @Override
    public int getMinStackSize(EntityType type) {
        return this.plugin.getConfig().getInt("Entities.Min Stack Amount", 1);
    }
}
