package com.songoda.core.hooks;

import com.songoda.core.hooks.hologram.HologramHook;
import com.songoda.core.hooks.hologram.HologramHookRegistry;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public class HookRegistryManager {
    private final Plugin plugin;

    private HologramHookRegistry hologramRegistry;

    public HookRegistryManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Optional<HologramHook> holograms() {
        return getHologramRegistry().getActive();
    }

    public HologramHookRegistry getHologramRegistry() {
        if (this.hologramRegistry == null) {
            this.hologramRegistry = new HologramHookRegistry(this.plugin);
            this.hologramRegistry.registerDefaultHooks();
        }

        return this.hologramRegistry;
    }

    public void deactivateAllActiveHooks() {
        if (this.hologramRegistry != null) {
            this.hologramRegistry.setActive(null);
        }
    }
}
