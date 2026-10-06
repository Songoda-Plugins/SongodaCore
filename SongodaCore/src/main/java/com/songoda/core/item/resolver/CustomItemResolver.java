package com.songoda.core.item.resolver;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction layer for resolving custom items from external item plugins
 * (such as Nexo, MMOItems, ItemsAdder, Oraxen, etc.) using material/item strings.
 *
 * <p>Implementations are annotated with {@link net.vortexdevelopment.vinject.annotation.component.Element}
 * and can declare {@link net.vortexdevelopment.vinject.annotation.DependsOn} to be discovered
 * and loaded conditionally when the target plugin is present.</p>
 */
public interface CustomItemResolver {

    /**
     * Primary prefix/namespace for this resolver (e.g. "nexo", "mmoitems", "itemsadder").
     *
     * @return the namespace prefix
     */
    @NotNull String getPrefix();

    /**
     * Returns true if this resolver matches the given prefix (case-insensitive).
     *
     * @param prefix the namespace prefix to test
     * @return true if this resolver handles the prefix
     */
    default boolean matchesPrefix(@NotNull String prefix) {
        return getPrefix().equalsIgnoreCase(prefix);
    }

    /**
     * Resolves an item stack given the remainder of the material string after the prefix.
     * For example, given "nexo:myitem", this method receives "myitem".
     * Given "mmoitems:SWORD:RUBY_SWORD", this method receives "SWORD:RUBY_SWORD".
     *
     * @param itemId the identifier portion after the prefix and colon
     * @return the resolved {@link ItemStack}, or null if could not be resolved
     */
    @Nullable ItemStack resolve(@NotNull String itemId);

    /**
     * Returns whether the provided item stack matches the given itemId for this provider.
     *
     * @param itemStack the item stack to check
     * @param itemId the custom item identifier
     * @return true if the item matches
     */
    boolean isItem(@NotNull ItemStack itemStack, @NotNull String itemId);

    /**
     * Extracts the custom item id from an existing item stack, or returns null if not from this provider.
     *
     * @param itemStack the item stack to inspect
     * @return the custom item id, or null
     */
    @Nullable String getItemId(@NotNull ItemStack itemStack);
}
