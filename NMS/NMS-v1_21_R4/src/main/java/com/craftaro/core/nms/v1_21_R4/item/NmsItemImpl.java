package com.craftaro.core.nms.v1_21_R4.item;

import com.craftaro.core.nms.item.NmsItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.v1_21_R4.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class NmsItemImpl implements NmsItem {
    private final RandomSource randomSource = RandomSource.createNewThreadLocalInstance();

    @Override
    public ItemStack copyAndApplyRandomEnchantment(ItemStack item, int level) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        EnchantmentHelper.enchantItem(this.randomSource, nmsItem, level, CraftRegistry.getMinecraftRegistry(), Optional.empty());
        return CraftItemStack.asBukkitCopy(nmsItem);
    }
}
