package com.songoda.core.hooks.hologram.adapter;

import com.songoda.core.hooks.hologram.HologramHook;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecentHologramsHook extends HologramHook {
    private static final String DECENT_HOLOGRAMS = "DecentHolograms";

    private final ArrayList<String> ourHologramIds = new ArrayList<>(0);
    private String hologramNamePrefix;

    @Override
    public String getName() {
        return DECENT_HOLOGRAMS;
    }

    @Override
    public @NotNull String[] getPluginDependencies() {
        return new String[] {DECENT_HOLOGRAMS};
    }

    @Override
    public void activate(Plugin plugin) {
        this.hologramNamePrefix = plugin.getClass().getName().replace('.', '_') + "-";
    }

    @Override
    public void deactivate() {
        removeAll();
        this.hologramNamePrefix = null;
    }

    @Override
    public boolean exists(@NotNull String id) {
        return DHAPI.getHologram(getHologramName(id)) != null;
    }

    @Override
    public void create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        if (exists(id)) {
            throw new IllegalStateException("Cannot create hologram that already exists: " + getHologramName(id));
        }

        DHAPI.createHologram(getHologramName(id), getNormalizedLocation(location), lines);
        this.ourHologramIds.add(id);
    }

    @Override
    public void update(@NotNull String id, @NotNull List<String> lines) {
        Hologram hologram = DHAPI.getHologram(getHologramName(id));
        if (hologram == null) {
            throw new IllegalStateException("Cannot update hologram that does not exist: " + getHologramName(id));
        }

        DHAPI.setHologramLines(hologram, lines);
    }

    @Override
    public void updateBulk(@NotNull Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove(@Nullable String id) {
        DHAPI.removeHologram(getHologramName(id));
        this.ourHologramIds.remove(id);
    }

    @Override
    public void removeAll() {
        for (String id : this.ourHologramIds) {
            DHAPI.removeHologram(getHologramName(id));
        }
        this.ourHologramIds.clear();
        this.ourHologramIds.trimToSize();
    }

    private String getHologramName(String id) {
        if (this.hologramNamePrefix == null) {
            throw new IllegalStateException("Hook has not been activated yet");
        }
        return this.hologramNamePrefix + id;
    }
}
