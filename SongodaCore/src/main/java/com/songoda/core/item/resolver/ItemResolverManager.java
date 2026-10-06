package com.songoda.core.item.resolver;

import net.vortexdevelopment.vinject.di.DependencyRepository;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry and manager for custom item resolvers.
 *
 * <p>Discovers all {@link CustomItemResolver} elements registered in the
 * VInject container and provides unified material string resolution.</p>
 */
@RegisterListener
public class ItemResolverManager implements Listener {

    private static final List<CustomItemResolver> resolvers = new CopyOnWriteArrayList<>();
    private static final List<CustomItemResolver> customRegisteredResolvers = new CopyOnWriteArrayList<>();

    static {
        reloadResolvers();
    }

    /**
     * Reloads and re-collects all {@link CustomItemResolver} elements from VInject.
     */
    public static void reloadResolvers() {
        resolvers.clear();
        try {
            Collection<CustomItemResolver> collected = DependencyRepository.getInstance().collectElements(CustomItemResolver.class);
            if (collected != null) {
                resolvers.addAll(collected);
            }
        } catch (Exception exception) {
            if (SongodaPlugin.getInstance() != null) {
                SongodaPlugin.getInstance().getLogger().warning("[ItemResolverManager] Failed to collect CustomItemResolver elements: " + exception.getMessage());
            }
        }

        for (CustomItemResolver customResolver : customRegisteredResolvers) {
            if (!resolvers.contains(customResolver)) {
                resolvers.add(customResolver);
            }
        }
    }

    /**
     * Programmatically registers a custom item resolver.
     *
     * @param resolver the resolver to register
     */
    public static void registerResolver(@NotNull CustomItemResolver resolver) {
        if (!customRegisteredResolvers.contains(resolver)) {
            customRegisteredResolvers.add(resolver);
        }
        if (!resolvers.contains(resolver)) {
            resolvers.add(resolver);
        }
    }

    /**
     * Unregisters a programmatically registered custom item resolver.
     *
     * @param resolver the resolver to unregister
     */
    public static void unregisterResolver(@NotNull CustomItemResolver resolver) {
        customRegisteredResolvers.remove(resolver);
        resolvers.remove(resolver);
    }

    /**
     * Returns an unmodifiable view of all registered item resolvers.
     *
     * @return the list of resolvers
     */
    @NotNull
    public static List<CustomItemResolver> getResolvers() {
        return Collections.unmodifiableList(resolvers);
    }

    /**
     * Finds the first resolver matching the given prefix.
     *
     * @param prefix the prefix to match
     * @return the matching resolver, or null
     */
    @Nullable
    public static CustomItemResolver getResolver(@NotNull String prefix) {
        for (CustomItemResolver resolver : resolvers) {
            if (resolver.matchesPrefix(prefix)) {
                return resolver;
            }
        }
        return null;
    }

    /**
     * Strips the {@code minecraft:} prefix from a material or item string if present.
     *
     * @param materialString the input string
     * @return the stripped string, or null if input was null
     */
    @Nullable
    public static String stripMinecraftPrefix(@Nullable String materialString) {
        if (materialString == null) {
            return null;
        }
        if (materialString.toLowerCase().startsWith("minecraft:")) {
            return materialString.substring("minecraft:".length());
        }
        return materialString;
    }

    /**
     * Resolves a material or custom item string into an {@link ItemStack}.
     *
     * <p>If the string starts with {@code minecraft:}, the prefix is stripped.
     * If the string contains a colon {@code :}, the substring before the first colon
     * is treated as the namespace prefix and dispatched to matching resolvers.
     * Otherwise, Bukkit's standard material resolution is used.</p>
     *
     * @param materialString the raw material or item identifier
     * @return the resolved {@link ItemStack}, or null if could not be resolved
     */
    @Nullable
    public static ItemStack resolve(@Nullable String materialString) {
        if (materialString == null || materialString.isBlank()) {
            return null;
        }

        String trimmed = stripMinecraftPrefix(materialString.trim());
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex >= 0) {
            String prefix = trimmed.substring(0, colonIndex);
            String itemId = trimmed.substring(colonIndex + 1);

            CustomItemResolver resolver = getResolver(prefix);
            if (resolver != null) {
                try {
                    ItemStack resolved = resolver.resolve(itemId);
                    if (resolved != null) {
                        return resolved;
                    }
                } catch (Exception exception) {
                    if (SongodaPlugin.getInstance() != null) {
                        SongodaPlugin.getInstance().getLogger().warning(
                                "[ItemResolverManager] Resolver " + resolver.getClass().getSimpleName()
                                        + " threw an exception resolving '" + trimmed + "': " + exception.getMessage());
                    }
                }
            }

            // Fallback: check if any other resolver can resolve the full string
            for (CustomItemResolver candidate : resolvers) {
                if (candidate != resolver && candidate.matchesPrefix(prefix)) {
                    try {
                        ItemStack resolved = candidate.resolve(itemId);
                        if (resolved != null) {
                            return resolved;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Vanilla material resolution
        Material material = Material.matchMaterial(trimmed);
        if (material != null) {
            try {
                if (material.isItem()) {
                    return new ItemStack(material);
                }
            } catch (Throwable throwable) {
                if (SongodaPlugin.getInstance() != null) {
                    SongodaPlugin.getInstance().getLogger().warning("[ItemResolverManager] Failed to create ItemStack for " + material + ": " + throwable.getMessage());
                }
            }
        }

        return null;
    }

    /**
     * Checks if the given item stack matches the specified material or custom item string.
     *
     * @param itemStack the item stack to check
     * @param materialString the material or custom item string
     * @return true if the item matches
     */
    public static boolean isCustomItem(@Nullable ItemStack itemStack, @Nullable String materialString) {
        if (itemStack == null || materialString == null || materialString.isBlank()) {
            return false;
        }

        String trimmed = materialString.trim();
        if (trimmed.toLowerCase().startsWith("minecraft:")) {
            trimmed = trimmed.substring("minecraft:".length()).trim();
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex >= 0) {
            String prefix = trimmed.substring(0, colonIndex);
            String itemId = trimmed.substring(colonIndex + 1);

            CustomItemResolver resolver = getResolver(prefix);
            if (resolver != null) {
                return resolver.isItem(itemStack, itemId);
            }
        }

        Material material = Material.matchMaterial(trimmed);
        return material != null && itemStack.getType() == material;
    }

    /**
     * Extracts the custom item identifier for the given item stack, if recognized by any registered resolver.
     *
     * @param itemStack the item stack to inspect
     * @return the namespaced custom item identifier (e.g. "nexo:my_item"), or null
     */
    @Nullable
    public static String getCustomItemId(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        for (CustomItemResolver resolver : resolvers) {
            String itemId = resolver.getItemId(itemStack);
            if (itemId != null && !itemId.isBlank()) {
                return resolver.getPrefix() + ":" + itemId;
            }
        }
        return null;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        reloadResolvers();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        reloadResolvers();
    }
}
