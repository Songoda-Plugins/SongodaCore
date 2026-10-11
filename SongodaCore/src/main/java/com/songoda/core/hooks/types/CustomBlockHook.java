package com.songoda.core.hooks.types;

import com.songoda.core.hooks.plugin.PluginHook;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for identifying custom blocks and their corresponding placeable items.
 *
 * <p>Custom block IDs returned by providers must use the provider's namespace in
 * the form {@code plugin_name:block_id}. The ID is kept as a string so consumers
 * do not need a compile-time dependency on any custom block plugin.</p>
 */
public abstract class CustomBlockHook extends PluginHook {

    /**
     * Returns the namespace used for this provider's custom block IDs.
     *
     * @return the provider namespace, such as {@code nexo} or {@code itemsadder}
     */
    public abstract @NotNull String getCustomBlockPrefix();

    /**
     * Returns the canonical custom block ID represented by a placed block.
     *
     * @param block the placed block to inspect
     * @return the namespaced custom block ID, or {@code null} when it is not handled
     */
    public abstract @Nullable String getCustomBlockId(@NotNull Block block);

    /**
     * Returns the canonical custom block ID represented by a placeable item.
     *
     * @param itemStack the item to inspect
     * @return the namespaced custom block ID, or {@code null} when it is not handled
     */
    public abstract @Nullable String getCustomBlockId(@NotNull ItemStack itemStack);

    /**
     * Creates the placeable item corresponding to a canonical custom block ID.
     *
     * @param customBlockId the namespaced custom block ID
     * @param amount the requested item amount
     * @return the custom block item, or {@code null} when the ID is not handled
     */
    public abstract @Nullable ItemStack createItemStack(@NotNull String customBlockId, int amount);

    /**
     * Checks whether a placed block is recognized by this provider.
     *
     * @param block the block to inspect
     * @return {@code true} when this provider recognizes the block
     */
    public boolean isCustomBlock(@NotNull Block block) {
        return getCustomBlockId(block) != null;
    }

    /**
     * Checks whether an item is a placeable custom block recognized by this provider.
     *
     * @param itemStack the item to inspect
     * @return {@code true} when this provider recognizes the item
     */
    public boolean isCustomBlockItem(@NotNull ItemStack itemStack) {
        return getCustomBlockId(itemStack) != null;
    }

    /**
     * Checks whether the held item represents the same custom block as the placed block.
     *
     * @param block the placed block
     * @param itemStack the candidate item
     * @return {@code true} when both resolve to the same custom block ID
     */
    public boolean canStack(@NotNull Block block, @NotNull ItemStack itemStack) {
        String blockId = getCustomBlockId(block);
        String itemId = getCustomBlockId(itemStack);
        return blockId != null && blockId.equalsIgnoreCase(itemId);
    }
}
