package com.songoda.core.nms.v1_18_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class NmsItemImpl implements NmsItem {
    @Override
    public ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentHelper.enchantItem(ThreadLocalRandom.current(), nmsItem, level, false);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
