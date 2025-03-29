package com.songoda.core.hooks.holograms;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.OutdatedHookInterface;
import com.songoda.core.hooks.hologram.HologramHook;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @deprecated See {@link SongodaPlugin#getHookManager()} and {@link HologramHook}
 */
@Deprecated
public abstract class Holograms implements OutdatedHookInterface {
    protected double xOffset = 0.5;
    protected double yOffset = 0.5;
    protected double zOffset = 0.5;

    protected final Plugin plugin;

    public Holograms(Plugin plugin) {
        this.plugin = plugin;
    }

    public Holograms setPositionOffset(double x, double y, double z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

        return this;
    }

    /**
     * Center and offset this location
     *
     * @param location location to offset
     *
     * @return copy-safe location with the applied offset.
     */
    protected final Location fixLocation(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return location.clone().add((x - (int) x) + xOffset, (y - (int) y) + yOffset + defaultHeightOffset(), (z - (int) z) + zOffset);
    }

    protected abstract double defaultHeightOffset();

    public void createHologram(String id, Location location, String line) {
        createHologram(id, location, Collections.singletonList(line));
    }

    public abstract void createHologram(String id, Location location, List<String> lines);

    public abstract void removeHologram(String id);

    public void updateHologram(String id, String line) {
        updateHologram(id, Collections.singletonList(line));
    }

    public abstract void updateHologram(String id, List<String> lines);

    public abstract void bulkUpdateHolograms(Map<String, List<String>> hologramData);

    public abstract void removeAllHolograms();

    public abstract boolean isHologramLoaded(String id);
}
