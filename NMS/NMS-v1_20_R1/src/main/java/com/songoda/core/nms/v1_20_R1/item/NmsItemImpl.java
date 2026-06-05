package com.songoda.core.nms.v1_20_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NmsItemImpl implements NmsItem {
    private final RandomSource randomSource = RandomSource.createNewThreadLocalInstance();

    @Override
    public ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentHelper.enchantItem(this.randomSource, nmsItem, level, false);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
