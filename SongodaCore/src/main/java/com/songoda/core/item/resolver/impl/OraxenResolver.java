package com.songoda.core.item.resolver.impl;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import com.songoda.core.item.resolver.CustomItemResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Element
@DependsOn(className = "io.th0rgal.oraxen.api.OraxenItems")
public class OraxenResolver implements CustomItemResolver {

    @Override
    public @NotNull String getPrefix() {
        return "oraxen";
    }

    @Override
    public @Nullable ItemStack resolve(@NotNull String itemId) {
        ItemBuilder builder = OraxenItems.getItemById(itemId);
        if (builder != null) {
            return builder.build();
        }
        return null;
    }

    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String itemId) {
        String id = OraxenItems.getIdByItem(itemStack);
        if (id != null) {
            return id.equalsIgnoreCase(itemId);
        }
        return false;
    }

    @Override
    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        return OraxenItems.getIdByItem(itemStack);
    }
}
