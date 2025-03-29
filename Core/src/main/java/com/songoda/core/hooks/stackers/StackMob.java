package com.songoda.core.hooks.stackers;

import com.songoda.core.SongodaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import uk.antiperson.stackmob.api.EntityManager;
import uk.antiperson.stackmob.api.StackedEntity;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class StackMob extends Stacker {
    private final EntityManager plugin;

    public StackMob() {
        this.plugin = new EntityManager((uk.antiperson.stackmob.StackMob) Bukkit.getPluginManager().getPlugin("StackMob"));
    }

    @Override
    public String getName() {
        return "StackMob";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsItemStacking() {
        return false;
    }

    @Override
    public boolean supportsEntityStacking() {
        return true;
    }

    /**
     * Don't do it.
     */
    @Override
    public void setItemAmount(Item item, int amount) {
        // idk, if you ignored the warnings and still use this method, we can at least try to help out
        item.getItemStack().setAmount(amount);
    }

    /**
     * If you use this method, you're pretty lazy. Didn't you see supportsItemStacking()?
     */
    @Override
    public int getItemAmount(Item item) {
        return item.getItemStack().getAmount();
    }

    @Override
    public boolean isStacked(LivingEntity entity) {
        return plugin.isStackedEntity(entity);
    }

    @Override
    public int getSize(LivingEntity entity) {
        return plugin.getStackedEntity(entity).getSize();
    }

    @Override
    public void remove(LivingEntity entity, int amount) {
        StackedEntity stackedEntity = plugin.getStackedEntity(entity);
        stackedEntity.setSize(stackedEntity.getSize() - amount);
    }

    @Override
    public void add(LivingEntity entity, int amount) {
        StackedEntity stackedEntity = plugin.getStackedEntity(entity);
        stackedEntity.setSize(stackedEntity.getSize() + amount);
    }

    @Override
    public int getMinStackSize(EntityType type) {
        return 0;
    }
}
