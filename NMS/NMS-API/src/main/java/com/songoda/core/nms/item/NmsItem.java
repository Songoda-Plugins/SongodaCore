package com.songoda.core.nms.item;

import org.bukkit.inventory.ItemStack;

public interface NmsItem {
    ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level);
}
