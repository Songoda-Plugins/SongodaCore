package com.songoda.core.hooks.internal;

import com.songoda.core.vinject.annotation.RegisterReloadHook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ReloadHookRegistry {

    private final Set<ReloadHook> hooks = new LinkedHashSet<>();

    public synchronized void register(ReloadHook hook) {
        hooks.add(hook);
    }

    synchronized List<ReloadHook> orderedHooks() {
        List<ReloadHook> ordered = new ArrayList<>(hooks);
        ordered.sort(Comparator.comparingInt((ReloadHook hook) -> hook instanceof ConfigReloadHook ? 0 : 1)
                .thenComparing(Comparator.comparingInt(ReloadHookRegistry::priority).reversed())
                .thenComparing(hook -> sourceClass(hook).getName()));
        return ordered;
    }

    public void reload() {
        for (ReloadHook hook : orderedHooks()) {
            hook.onReload();
        }
    }

    private static Class<?> sourceClass(ReloadHook hook) {
        return hook instanceof ConfigReloadHook configHook ? configHook.sourceClass() : hook.getClass();
    }

    private static int priority(ReloadHook hook) {
        RegisterReloadHook annotation = sourceClass(hook).getAnnotation(RegisterReloadHook.class);
        return annotation != null ? annotation.priority() : 10;
    }
}
