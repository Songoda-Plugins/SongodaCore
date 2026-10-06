package com.songoda.core.hooks.types;

import com.songoda.core.hooks.plugin.PluginHook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves namespaced item references used by the SongodaCore ItemStack serializer.
 *
 * <p>The reference is deliberately kept as a string so providers can use their own
 * item API without SongodaCore depending on a particular item plugin.</p>
 *
 * @deprecated Replaced by {@link com.songoda.core.item.resolver.CustomItemResolver}
 * and {@link com.songoda.core.item.resolver.ItemResolverManager}.
 */
@Deprecated
public abstract class ItemResolverHook extends PluginHook {

    /**
     * Returns whether this provider owns the supplied reference.
     */
    public abstract boolean canResolve(@NotNull String reference);

    /**
     * Resolves a reference into a new item stack. The serializer applies the
     * configured amount after this method returns.
     */
    public abstract @Nullable ItemStack resolve(@NotNull String reference);
}
