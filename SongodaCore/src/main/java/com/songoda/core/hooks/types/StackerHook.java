package com.songoda.core.hooks.types;

import com.songoda.core.hooks.plugin.PluginHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public abstract class StackerHook extends PluginHook {

    /**
     * Gets the real amount of items represented by the given Item entity.
     *
     * @param item The Item entity to check.
     * @return The real amount of items
     */
    @NotNull
    public abstract BigInteger getRealAmount(@NotNull Item item);

    /**
     * Creates a stacked item from an existing item entity with the given amount.
     *
     * @param item   The item entity to stack.
     * @param amount The amount to stack.
     */
    public abstract void createStackedItem(@NotNull Item item, @NotNull BigInteger amount);

    /**
     * Creates a stacked item at the specified location with the given amount.
     *
     * @param itemStack The item stack to stack.
     * @param location  The location to spawn the stacked item.
     * @param amount    The amount to stack.
     */
    public abstract void createStackedItem(@NotNull Location location, @NotNull ItemStack itemStack, @NotNull BigInteger amount);

    /**
     * Checks if an entity type can be stacked.
     *
     * @param entityType entity type to check
     * @return {@code true} if the type is eligible for stacking
     */
    public boolean canStack(@NotNull EntityType entityType) {
        throw new UnsupportedOperationException("Entity stacking is not supported by this hook");
    }

    /**
     * Checks if a living entity can be stacked.
     *
     * @param entity entity to check
     * @return {@code true} if the entity is eligible for stacking
     */
    public boolean canStack(@NotNull LivingEntity entity) {
        throw new UnsupportedOperationException("Entity stacking is not supported by this hook");
    }

    /**
     * Gets the amount represented by a living entity stack.
     *
     * @param entity entity to check
     * @return amount represented by the entity
     */
    public @NotNull BigInteger getEntityStackAmount(@NotNull LivingEntity entity) {
        throw new UnsupportedOperationException("Entity stacking is not supported by this hook");
    }

    /**
     * Creates or registers an entity stack for a living entity.
     *
     * @param entity entity to track
     * @param amount initial amount represented by the stack
     */
    public void createStackedEntity(@NotNull LivingEntity entity, @NotNull BigInteger amount) {
        throw new UnsupportedOperationException("Entity stacking is not supported by this hook");
    }

    /**
     * Gets the amount represented by a spawner stack at the given location.
     * Returns {@code null} when the location is not a stacked spawner.
     */
    public @Nullable BigInteger getSpawnerStackAmount(@NotNull Location location) {
        throw new UnsupportedOperationException("Spawner stacking is not supported by this hook");
    }

    /**
     * Creates or registers a spawner stack for a placed spawner.
     */
    public void createStackedSpawner(@NotNull BlockPlaceEvent event) {
        throw new UnsupportedOperationException("Spawner stacking is not supported by this hook");
    }

    /**
     * Gets the amount represented by a block stack at the given location.
     * Returns {@code null} when the location is not a stacked block.
     */
    public @Nullable BigInteger getBlockStackAmount(@NotNull Location location) {
        throw new UnsupportedOperationException("Block stacking is not supported by this hook");
    }

    /**
     * Creates or registers a block stack at the given location.
     */
    public void createStackedBlock(@NotNull Location location, @NotNull Material material, @NotNull BigInteger amount) {
        throw new UnsupportedOperationException("Block stacking is not supported by this hook");
    }
}
