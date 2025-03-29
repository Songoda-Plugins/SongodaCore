package com.songoda.core.nms.v1_17_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class NmsItemImpl implements NmsItem {
    @Override
    public ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentManager.a(ThreadLocalRandom.current(), nmsItem, level, false);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
