package com.songoda.core.item.resolver.impl;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import com.songoda.core.item.resolver.CustomItemResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Element
@DependsOn(className = "com.nexomc.nexo.api.NexoItems")
public class NexoItemResolver implements CustomItemResolver {

    @Override
    public @NotNull String getPrefix() {
        return "nexo";
    }

    @Override
    public @Nullable ItemStack resolve(@NotNull String itemId) {
        ItemBuilder builder = NexoItems.itemFromId(itemId);
        if (builder != null) {
            return builder.build();
        }
        return null;
    }

    @Override
    public boolean isItem(@NotNull ItemStack itemStack, @NotNull String itemId) {
        String id = NexoItems.idFromItem(itemStack);
        if (id != null) {
            return id.equalsIgnoreCase(itemId);
        }
        return false;
    }

    @Override
    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        return NexoItems.idFromItem(itemStack);
    }
}
