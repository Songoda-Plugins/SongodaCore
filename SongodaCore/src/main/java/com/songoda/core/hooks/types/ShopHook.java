package com.songoda.core.hooks.types;

import com.songoda.core.hooks.plugin.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopHook extends PluginHook {

    public abstract double getPrice(ItemStack itemStack);

    public double getPrice(ItemStack itemStack, UUID player) {
        Player p = Bukkit.getPlayer(player);
        if (p == null) {
            return getPrice(itemStack);
        } else {
            return getPrice(itemStack, p);
        }
    }

    public abstract double getPrice(ItemStack itemStack, Player player);

    public abstract boolean canSell(ItemStack itemStack);

}
