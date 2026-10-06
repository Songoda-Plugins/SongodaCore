package com.songoda.core.hooks.plugin.shop;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import com.songoda.core.hooks.types.ShopHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Element
@DependsOn(ShopGuiPlusApi.class)
public class ShopGUIPlusHook extends ShopHook {

    @Override
    public double getPrice(ItemStack itemStack) {
        ItemStack cleanStack = itemStack.clone();
        cleanStack.setAmount(1);
        return ShopGuiPlusApi.getItemStackPriceSell(cleanStack);
    }

    @Override
    public double getPrice(ItemStack itemStack, Player player) {
        ItemStack cleanStack = itemStack.clone();
        cleanStack.setAmount(1);
        return ShopGuiPlusApi.getItemStackPriceSell(player, cleanStack);
    }

    @Override
    public boolean canSell(ItemStack itemStack) {
        ItemStack cleanStack = itemStack.clone();
        cleanStack.setAmount(1);
        return ShopGuiPlusApi.getItemStackPriceSell(cleanStack) != -1.0;
    }

    @Override
    public String getRequiredPlugin() {
        return "ShopGUIPlus";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
