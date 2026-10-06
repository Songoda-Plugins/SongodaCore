package com.songoda.core.hooks.types;

import com.songoda.core.hooks.plugin.PluginHook;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
}
