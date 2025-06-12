package com.songoda.core.hooks;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This hook registry makes use of priorities to automatically activate the highest priority hook that is available if no hook has been activated programmatically.
 */
// TODO: Allow multiple hooks to be active at the same time (useful for using multiple specific Eco/Protection/... hooks/plugins)
// TODO: Allow specifying a hook-name-string as "prefered" to be auto-active (maybe String[] or comma-separated String?)
//       (null means use priority, otherwise try to activate the hook with the given name first)
public abstract class BaseHookRegistry<T extends Hook> extends HookRegistry<T> {
    private final Plugin plugin;

    private final Map<T, Integer> hooksWithPriority = new HashMap<>();
    protected T activeHook = null;

    public BaseHookRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public abstract void registerDefaultHooks();

    public Optional<T> getActive() {
        if (this.activeHook == null) {
            T hook = findFirstAvailableHook();
            if (hook != null) {
                setActive(hook);
                this.plugin.getLogger().info("Activated hook '" + hook.getName() + "'");
            }

            checkDependenciesOfAllHooksAndLogMissingOnes();
        }
        return Optional.ofNullable(this.activeHook);
    }

    public void setActive(@Nullable T hook) {
        if (this.activeHook == hook) {
            return;
        }

        if (this.activeHook != null) {
            this.activeHook.deactivate();
        }

        this.activeHook = hook;
        if (this.activeHook != null) {
            this.activeHook.activate(this.plugin);
        }
    }

    @Override
    public @Nullable T get(String name) {
        for (T hook : this.hooksWithPriority.keySet()) {
            if (hook.getName().equalsIgnoreCase(name)) {
                return hook;
            }
        }
        return null;
    }

    @Override
    public @NotNull List<T> getAll() {
        // Use List.copyOf() when we upgrade to Java 10+
        return Collections.unmodifiableList(new ArrayList<>(this.hooksWithPriority.keySet()));
    }

    @Override
    public @NotNull List<String> getAllNames() {
        return this.hooksWithPriority
                .keySet()
                .stream()
                .map(Hook::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public void register(@NotNull T hook) {
        register(hook, 0);
    }

    /**
     * @see HookPriority
     */
    public void register(@NotNull T hook, int priority) {
        if (get(hook.getName()) != null) {
            throw new IllegalArgumentException("Hook with name '" + hook.getName() + "' already registered");
        }
        this.hooksWithPriority.put(hook, priority);
    }

    @Override
    public void unregister(@NotNull T hook) {
        if (this.activeHook == hook) {
            this.activeHook = null;
            hook.deactivate();
        }
        this.hooksWithPriority.remove(hook);
    }

    @Override
    public void clear() {
        this.hooksWithPriority.clear();
    }

    protected @Nullable T findFirstAvailableHook() {
        return this.hooksWithPriority
                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .filter((entry) -> entry.getKey().canBeActivated())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    protected void checkDependenciesOfAllHooksAndLogMissingOnes() {
        List<String> missingDependencies = new ArrayList<>(0);

        for (T hook : getAll()) {
            for (String pluginName : hook.getPluginDependencies()) {
                if (this.plugin.getDescription().getDepend().contains(pluginName)) {
                    continue;
                }
                if (this.plugin.getDescription().getSoftDepend().contains(pluginName)) {
                    continue;
                }

                missingDependencies.add(pluginName);
            }
        }

        if (!missingDependencies.isEmpty()) {
            this.plugin.getLogger().warning("Nag author(s): Plugin accesses hooks that it does not declare dependance on: " + String.join(", ", missingDependencies));
        }
    }
}
