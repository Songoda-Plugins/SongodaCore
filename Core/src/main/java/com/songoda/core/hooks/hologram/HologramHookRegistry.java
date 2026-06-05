package com.songoda.core.hooks.hologram;

import com.songoda.core.hooks.BaseHookRegistry;
import com.songoda.core.hooks.HookPriority;
import com.songoda.core.hooks.hologram.adapter.CmiHologramHook;
import com.songoda.core.hooks.hologram.adapter.DecentHologramsHook;
import com.songoda.core.hooks.hologram.adapter.SainttxHologramsHook;
import org.bukkit.plugin.Plugin;

public class HologramHookRegistry extends BaseHookRegistry<HologramHook> {
    public HologramHookRegistry(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void registerDefaultHooks() {
        register(new DecentHologramsHook(), HookPriority.HIGH);
        register(new SainttxHologramsHook(), HookPriority.NORMAL);
        register(new CmiHologramHook(), HookPriority.LOW);
    }
}
