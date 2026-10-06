package com.songoda.core.item.resolver.impl;

import net.Indyuce.mmoitems.MMOItems;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import com.songoda.core.item.resolver.CustomItemResolver;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Element
@DependsOn(className = "net.Indyuce.mmoitems.MMOItems")
public class MMOItemsResolver implements CustomItemResolver {

    @Override
    public @NotNull String getPrefix() {
        return "mmoitems";
    }

    @Override
    public @Nullable ItemStack resolve(@NotNull String itemId) {
        int colonIndex = itemId.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }

        String type = itemId.substring(0, colonIndex);
        String id = itemId.substring(colonIndex + 1);

        if (MMOItems.plugin != null) {
            return MMOItems.plugin.getItem(type, id);
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
        String type = MMOItems.getTypeName(itemStack);
        String id = MMOItems.getID(itemStack);
        if (type != null && !type.isBlank() && id != null && !id.isBlank()) {
            return type + ":" + id;
        }
        return null;
    }
}
