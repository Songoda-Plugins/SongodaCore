package com.songoda.core.hooks.plugin.customblock;

import com.songoda.core.hooks.types.CustomBlockHook;
import dev.lone.itemsadder.api.CustomBlock;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.component.Element;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** SongodaCore custom block integration for ItemsAdder. */
@Element
@DependsOn(className = "dev.lone.itemsadder.api.CustomBlock")
public final class ItemsAdderCustomBlockHook extends CustomBlockHook {

    @Override
    public @NotNull String getCustomBlockPrefix() {
        return "itemsadder";
    }

    @Override
    public @Nullable String getCustomBlockId(@NotNull Block block) {
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        return customBlock == null ? null : getCustomBlockPrefix() + ":" + customBlock.getNamespacedID();
    }

    @Override
    public @Nullable String getCustomBlockId(@NotNull ItemStack itemStack) {
        CustomBlock customBlock = CustomBlock.byItemStack(itemStack);
        return customBlock == null ? null : getCustomBlockPrefix() + ":" + customBlock.getNamespacedID();
    }

    @Override
    public @Nullable ItemStack createItemStack(@NotNull String customBlockId, int amount) {
        String itemId = getItemId(customBlockId);
        if (itemId == null) {
            return null;
        }
        CustomBlock customBlock = CustomBlock.getInstance(itemId);
        if (customBlock == null) {
            return null;
        }
        ItemStack itemStack = customBlock.getItemStack();
        if (itemStack == null) {
            return null;
        }
        itemStack.setAmount(amount);
        return itemStack;
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "ItemsAdder";
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
