package com.songoda.core.hooks.plugin;


import net.vortexdevelopment.vinject.annotation.component.Element;
import net.vortexdevelopment.vinject.di.DependencyRepository;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.types.ShopHook;
import com.songoda.core.hooks.types.StackerHook;
import com.songoda.core.hooks.types.CustomBlockHook;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RegisterListener
public class HookManager implements Listener {

    private final static Set<PluginHook> pluginHooks = new LinkedHashSet<>();

    static {
        pluginHooks.addAll(DependencyRepository.getInstance().collectElements(PluginHook.class));
        reloadHooks();
    }

    public static int getHookPriority(Class<? extends PluginHook> hookClass) {
        return pluginHooks.stream()
                .filter(hookClass::isInstance)
                .findFirst()
                .map(HookManager::getPriority)
                .orElse(0);
    }

    private static <T extends PluginHook> List<T> getHookByType(Class<T> hookClass) {
        return pluginHooks.stream()
                .filter(hookClass::isInstance)
                .map(hookClass::cast)
                .toList();
    }

    private static void reloadHooks() {
        reloadHooks(ShopHook.class);
        reloadHooks(StackerHook.class);
        reloadAllHooks(CustomBlockHook.class);
    }

    private static <T extends PluginHook> void reloadAllHooks(Class<T> type) {
        for (T hook : getHookByType(type)) {
            if (hook.canEnable()) {
                if (!hook.isEnabled()) {
                    hook.onEnable();
                    hook.setEnabled(true);
                }
            } else if (hook.isEnabled()) {
                hook.onDisable();
                hook.setEnabled(false);
            }
        }
    }

    private static <T extends PluginHook> void reloadHooks(Class<T> type) {
        List<T> hooks = getHookByType(type);
        T bestHook = hooks.stream()
                .filter(PluginHook::canEnable)
                .max(Comparator.comparingInt(HookManager::getPriority))
                .orElse(null);

        for (T hook : hooks) {
            if (hook == bestHook) {
                if (!hook.isEnabled()) {
                    hook.onEnable();
                    hook.setEnabled(true);
                    SongodaPlugin.getInstance().getLogger().info("[HookManager] [Priority] Enabled " + type.getSimpleName() + " hook: " + hook.getClass().getSimpleName() + " (Priority: " + getPriority(hook) + ")");
                }
            } else {
                if (hook.isEnabled()) {
                    hook.onDisable();
                    hook.setEnabled(false);
                    SongodaPlugin.getInstance().getLogger().info("[HookManager] [Priority] Disabled " + type.getSimpleName() + " hook: " + hook.getClass().getSimpleName() + " (as a better hook exists or it's unavailable)");
                }
            }
        }
    }

    private static int getPriority(PluginHook hook) {
        return hook.getClass().isAnnotationPresent(Element.class) ? hook.getClass().getAnnotation(Element.class).priority() : 0;
    }

    public static ShopHook getEnabledShopHook() {
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.isEnabled()) {
                return shopHook;
            }
        }
        return null;
    }

    public static void setEnabledShopHook(String pluginName) {
        //Disable all hooks
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                shopHook.onEnable();
                shopHook.setEnabled(true);
            } else if (shopHook.isEnabled()) {
                shopHook.onDisable();
                shopHook.setEnabled(false);
            }
        }
    }

    public static StackerHook getEnabledStackerHook() {
        for (StackerHook stackerHook : getHookByType(StackerHook.class)) {
            if (stackerHook.isEnabled()) {
                return stackerHook;
            }
        }
        return null;
    }

    /**
     * Returns the canonical custom block ID for a placed block from the first active provider that recognizes it.
     *
     * @param block the placed block to inspect
     * @return the provider-prefixed custom block ID, or {@code null} when no provider recognizes the block
     */
    public static @Nullable String getCustomBlockId(@NotNull Block block) {
        for (CustomBlockHook hook : getHookByType(CustomBlockHook.class)) {
            if (!hook.isEnabled()) {
                continue;
            }
            try {
                String customBlockId = hook.getCustomBlockId(block);
                if (customBlockId != null) {
                    return customBlockId;
                }
            } catch (RuntimeException exception) {
                logCustomBlockHookFailure(hook, "identify placed block", exception);
            }
        }
        return null;
    }

    /**
     * Returns the canonical custom block ID for an item from the first active provider that recognizes it.
     *
     * @param itemStack the candidate placeable item
     * @return the provider-prefixed custom block ID, or {@code null} when no provider recognizes the item
     */
    public static @Nullable String getCustomBlockId(@NotNull ItemStack itemStack) {
        for (CustomBlockHook hook : getHookByType(CustomBlockHook.class)) {
            if (!hook.isEnabled()) {
                continue;
            }
            try {
                String customBlockId = hook.getCustomBlockId(itemStack);
                if (customBlockId != null) {
                    return customBlockId;
                }
            } catch (RuntimeException exception) {
                logCustomBlockHookFailure(hook, "identify item", exception);
            }
        }
        return null;
    }

    /**
     * Checks whether the supplied block is recognized as a custom block by an active provider.
     *
     * @param block the placed block to inspect
     * @return {@code true} when an active provider recognizes the block
     */
    public static boolean isCustomBlock(@NotNull Block block) {
        return getCustomBlockId(block) != null;
    }

    /**
     * Checks whether the supplied item represents a placeable custom block recognized by an active provider.
     *
     * @param itemStack the item to inspect
     * @return {@code true} when an active provider recognizes the item
     */
    public static boolean isCustomBlockItem(@NotNull ItemStack itemStack) {
        return getCustomBlockId(itemStack) != null;
    }

    /**
     * Checks whether a candidate item represents the same custom block as the placed block.
     *
     * @param block the placed block
     * @param itemStack the candidate item
     * @return {@code true} when both resolve to the same provider-prefixed ID
     */
    public static boolean canStack(@NotNull Block block, @NotNull ItemStack itemStack) {
        String blockId = getCustomBlockId(block);
        String itemId = getCustomBlockId(itemStack);
        return blockId != null && blockId.equalsIgnoreCase(itemId);
    }

    /**
     * Creates a placeable custom block item from a provider-prefixed custom block ID.
     *
     * @param customBlockId the canonical provider-prefixed ID
     * @param amount the requested item amount
     * @return the custom item stack, or {@code null} when there is no active provider for the ID
     */
    public static @Nullable ItemStack createCustomBlockItemStack(@NotNull String customBlockId, int amount) {
        int separator = customBlockId.indexOf(':');
        if (separator <= 0 || separator == customBlockId.length() - 1) {
            return null;
        }
        String prefix = customBlockId.substring(0, separator);
        for (CustomBlockHook hook : getHookByType(CustomBlockHook.class)) {
            if (!hook.isEnabled() || !hook.getCustomBlockPrefix().equalsIgnoreCase(prefix)) {
                continue;
            }
            try {
                return hook.createItemStack(customBlockId, amount);
            } catch (RuntimeException exception) {
                logCustomBlockHookFailure(hook, "create item for " + customBlockId, exception);
                return null;
            }
        }
        return null;
    }

    private static void logCustomBlockHookFailure(CustomBlockHook hook, String operation, RuntimeException exception) {
        if (SongodaPlugin.getInstance() != null) {
            SongodaPlugin.getInstance().getLogger().warning("[HookManager] Custom block hook "
                    + hook.getClass().getSimpleName() + " failed to " + operation + ": " + exception.getMessage());
        }
    }

    public static boolean hasShopHook(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            return false;
        }
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return true;
            }
        }
        return false;
    }

    public static ShopHook getShopHook(String pluginName) {
        for (ShopHook shopHook : getHookByType(ShopHook.class)) {
            if (shopHook.getRequiredPlugin().equalsIgnoreCase(pluginName)) {
                return shopHook;
            }
        }
        return null;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        reloadHooks();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        reloadHooks();
    }
}
