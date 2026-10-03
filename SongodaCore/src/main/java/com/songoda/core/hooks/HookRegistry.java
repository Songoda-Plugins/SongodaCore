package com.songoda.core.hooks;

import com.songoda.core.hooks.protection.GriefPreventionProtectionHook;
import com.songoda.core.hooks.protection.ProtectionHook;
import com.songoda.core.hooks.protection.WorldGuardProtectionHook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Runs optional plugin integrations only when their plugin is enabled and any supplied condition passes.
 */
public final class HookRegistry {

    private final Map<String, RegisteredProtectionHook> protectionHooks = new ConcurrentHashMap<>();

    public HookRegistry() {
        registerProtectionPlugin("WorldGuard");
        registerProtectionPlugin("GriefPrevention");
    }

    private void registerProtectionPlugin(String pluginName) {
        this.protectionHooks.put(pluginName.toLowerCase(Locale.ROOT), new RegisteredProtectionHook(pluginName, null));
    }

    /**
     * Registers a protection provider for an optional plugin.
     *
     * @param pluginName name of the optional plugin
     * @param hookFactory factory for the provider, called only while the plugin is enabled
     */
    public void registerProtectionHook(@NotNull String pluginName, @NotNull Supplier<? extends ProtectionHook> hookFactory) {
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(hookFactory, "hookFactory");
        if (pluginName.isBlank()) {
            throw new IllegalArgumentException("pluginName must not be blank");
        }
        this.protectionHooks.put(pluginName.toLowerCase(Locale.ROOT), new RegisteredProtectionHook(pluginName, hookFactory));
    }

    /**
     * Checks whether a player can break at a location according to every registered protection plugin that is enabled.
     *
     * @param player player attempting the action
     * @param location target location
     * @return false if any available provider denies the action
     */
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        return checkProtectionHooks(player, location, ProtectionHook::canBreak);
    }

    /**
     * Checks whether a player can interact at a location according to every registered protection plugin that is enabled.
     *
     * @param player player attempting the action
     * @param location target location
     * @return false if any available provider denies the action
     */
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        return checkProtectionHooks(player, location, ProtectionHook::canInteract);
    }

    private boolean checkProtectionHooks(Player player, Location location, ProtectionCheck check) {
        for (RegisteredProtectionHook entry : this.protectionHooks.values()) {
            Optional<Boolean> allowed = checkIfAvailable(entry.pluginName(), () -> true, plugin -> {
                ProtectionHook hook = entry.getHook();
                return check.isAllowed(hook, player, location);
            });
            if (allowed.isPresent() && !allowed.get()) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface ProtectionCheck {
        boolean isAllowed(ProtectionHook hook, Player player, Location location);
    }

    private static final class RegisteredProtectionHook {

        private final String pluginName;
        private final Supplier<? extends ProtectionHook> hookFactory;
        private ProtectionHook hook;

        private RegisteredProtectionHook(String pluginName, Supplier<? extends ProtectionHook> hookFactory) {
            this.pluginName = pluginName;
            this.hookFactory = hookFactory;
        }

        private String pluginName() {
            return this.pluginName;
        }

        private synchronized ProtectionHook getHook() {
            if (this.hook == null) {
                this.hook = this.hookFactory != null
                        ? this.hookFactory.get()
                        : switch (this.pluginName) {
                            case "WorldGuard" -> new WorldGuardProtectionHook();
                            case "GriefPrevention" -> new GriefPreventionProtectionHook();
                            default -> throw new IllegalStateException("No protection hook registered for " + this.pluginName);
                        };
            }
            return this.hook;
        }
    }

    /**
     * Runs an integration callback when the named plugin is enabled.
     *
     * @param pluginName name of the optional plugin
     * @param callback callback to run with the enabled plugin
     * @return true when the callback ran
     */
    public boolean runIfAvailable(@NotNull String pluginName, @NotNull Consumer<Plugin> callback) {
        return runIfAvailable(pluginName, () -> true, callback);
    }

    /**
     * Runs an integration callback when the named plugin is enabled and the condition passes.
     * The condition is evaluated first, so disabled features do not perform plugin lookups or callback work.
     *
     * @param pluginName name of the optional plugin
     * @param condition condition that enables the integration
     * @param callback callback to run with the enabled plugin
     * @return true when the callback ran
     */
    public boolean runIfAvailable(@NotNull String pluginName,
                                  @NotNull BooleanSupplier condition,
                                  @NotNull Consumer<Plugin> callback) {
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(callback, "callback");

        if (pluginName.isBlank() || !condition.getAsBoolean()) {
            return false;
        }

        Plugin plugin = findPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }

        callback.accept(plugin);
        return true;
    }

    /**
     * Runs a check when the named plugin is enabled and the condition passes.
     * An empty result means the plugin or condition was unavailable; otherwise the value is the check result.
     *
     * @param pluginName name of the optional plugin
     * @param condition condition that enables the integration
     * @param check check to run with the enabled plugin
     * @return the check result, or empty when the integration should not run
     */
    public @NotNull Optional<Boolean> checkIfAvailable(@NotNull String pluginName,
                                                       @NotNull BooleanSupplier condition,
                                                       @NotNull Predicate<Plugin> check) {
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(check, "check");

        if (pluginName.isBlank() || !condition.getAsBoolean()) {
            return Optional.empty();
        }

        Plugin plugin = findPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(check.test(plugin));
    }

    /**
     * Returns whether a plugin is enabled on this server.
     *
     * @param pluginName name of the optional plugin
     * @return true when the plugin is enabled
     */
    public boolean isAvailable(@NotNull String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName");
        Plugin plugin = pluginName.isBlank() ? null : findPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    private Plugin findPlugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null && pluginName.equalsIgnoreCase("AureliumSkills")) {
            plugin = Bukkit.getPluginManager().getPlugin("AuraSkills");
        }
        return plugin;
    }
}
