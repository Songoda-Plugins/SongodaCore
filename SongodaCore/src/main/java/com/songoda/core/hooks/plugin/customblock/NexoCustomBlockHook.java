package com.songoda.core.hooks.plugin.customblock;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.items.ItemBuilder;
import com.songoda.core.hooks.types.CustomBlockHook;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** SongodaCore custom block integration for Nexo. */
@Element
@DependsOn(className = "com.nexomc.nexo.api.NexoBlocks")
public final class NexoCustomBlockHook extends CustomBlockHook {

    @Override
    public @NotNull String getCustomBlockPrefix() {
        return "nexo";
    }

    @Override
    public @Nullable String getCustomBlockId(@NotNull Block block) {
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
        return mechanic == null ? null : getCustomBlockPrefix() + ":" + mechanic.getItemID();
    }

    @Override
    public @Nullable String getCustomBlockId(@NotNull ItemStack itemStack) {
        if (!NexoBlocks.isCustomBlock(itemStack)) {
            return null;
        }
        String itemId = NexoItems.idFromItem(itemStack);
        return itemId == null ? null : getCustomBlockPrefix() + ":" + itemId;
    }

    @Override
    public @Nullable ItemStack createItemStack(@NotNull String customBlockId, int amount) {
        String itemId = getItemId(customBlockId);
        if (itemId == null || !NexoBlocks.isCustomBlock(itemId)) {
            return null;
        }
        ItemBuilder builder = NexoItems.itemFromId(itemId);
        if (builder == null) {
            return null;
        }
        return builder.setAmount(amount).build();
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "Nexo";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    private @Nullable String getItemId(String customBlockId) {
        String prefix = getCustomBlockPrefix() + ":";
        if (!customBlockId.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return customBlockId.substring(prefix.length());
    }
}
