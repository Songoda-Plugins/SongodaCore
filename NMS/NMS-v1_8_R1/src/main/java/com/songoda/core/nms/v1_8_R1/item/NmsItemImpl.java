package com.songoda.core.nms.v1_8_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.server.v1_8_R1.EnchantmentManager;
import net.minecraft.server.v1_8_R1.ItemStack;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class NmsItemImpl implements NmsItem {
    @Override
    public org.bukkit.inventory.ItemStack copyAndApplyRandomEnchantment(org.bukkit.inventory.ItemStack item, int level) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentManager.a(ThreadLocalRandom.current(), nmsItem, level);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
