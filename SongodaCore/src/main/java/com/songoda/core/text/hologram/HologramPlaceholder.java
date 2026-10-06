package com.songoda.core.text.hologram;

import com.songoda.core.text.MiniMessagePlaceholder;

class HologramPlaceholder {

    private final HologramPlaceholderProvider provider;
    private final boolean async;
    private volatile MiniMessagePlaceholder value;
    private long lastUpdate = 0;
    private long updateIntervalTicks = 0;

    HologramPlaceholder(HologramPlaceholderProvider provider, long updateIntervalTicks, boolean async) {
        this.provider = provider;
        this.updateIntervalTicks = updateIntervalTicks;
        this.async = async;
        this.value = provider.getPlaceholder();
    }

    MiniMessagePlaceholder getPlaceholder() {
        return value;
    }

    MiniMessagePlaceholder refresh() {
        MiniMessagePlaceholder next = provider.getPlaceholder();
        value = next;
        return next;
    }

    boolean isAsync() {
        return async;
    }

    boolean shouldUpdate() {
        return shouldUpdate(false);
    }

    boolean shouldUpdate(boolean force) {
        if (updateIntervalTicks == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (force || now - lastUpdate >= updateIntervalTicks * 50L) {
            lastUpdate = now;
            return true;
        }
        return false;
    }

}
