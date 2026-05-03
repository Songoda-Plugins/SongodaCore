package com.songoda.core.nms.v26_1_R1.item;

import com.songoda.core.nms.item.NmsItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class NmsItemImpl implements NmsItem {
    private final RandomSource randomSource = RandomSource.createThreadLocalInstance();

    @Override
    public ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentHelper.enchantItem(this.randomSource, nmsItem, level, CraftRegistry.getMinecraftRegistry(), Optional.empty());
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
