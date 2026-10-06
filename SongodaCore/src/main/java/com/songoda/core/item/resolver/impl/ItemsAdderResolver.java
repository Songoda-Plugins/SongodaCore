package com.songoda.core.item.resolver.impl;

import dev.lone.itemsadder.api.CustomStack;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import com.songoda.core.item.resolver.CustomItemResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Element
@DependsOn(className = "dev.lone.itemsadder.api.CustomStack")
public class ItemsAdderResolver implements CustomItemResolver {

    @Override
    public @NotNull String getPrefix() {
        return "itemsadder";
    }

    @Override
    public boolean matchesPrefix(@NotNull String prefix) {
        return prefix.equalsIgnoreCase("itemsadder") || prefix.equalsIgnoreCase("ia");
    }

    @Override
    public @Nullable ItemStack resolve(@NotNull String itemId) {
        CustomStack customStack = CustomStack.getInstance(itemId);
        if (customStack != null) {
            return customStack.getItemStack();
        }
        return null;
    }

    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String itemId) {
        String currentId = getItemId(itemStack);
        if (currentId != null) {
            return currentId.equalsIgnoreCase(itemId);
        }
        return false;
    }

    @Override
    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        CustomStack customStack = CustomStack.byItemStack(itemStack);
        if (customStack != null) {
            return customStack.getNamespacedID();
        }
        return null;
    }
}
