package com.songoda.core.nms.v1_13_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.server.v1_13_R1.EnchantmentManager;
import net.minecraft.server.v1_13_R1.ItemStack;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class NmsItemImpl implements NmsItem {
    @Override
    public org.bukkit.inventory.ItemStack copyAndApplyRandomEnchantment(org.bukkit.inventory.ItemStack item, int level) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentManager.a(ThreadLocalRandom.current(), nmsItem, level, false);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
