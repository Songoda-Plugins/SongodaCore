package com.songoda.core.hooks.holograms;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.hologram.HologramHook;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated See {@link SongodaPlugin#getHookManager()} and {@link HologramHook}
 */
@Deprecated
public class DecentHologramsHolograms extends Holograms {
    private final Set<String> ourHolograms = new HashSet<>();

    public DecentHologramsHolograms(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "DecentHolograms";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected double defaultHeightOffset() {
        return 1;
    }

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        createAt(id, location, lines);
    }

    @Override
    public void removeHologram(String id) {
        Hologram hologram = DHAPI.getHologram(id);

        if (hologram != null) {
            hologram.delete();
            DecentHologramsAPI.get().getHologramManager().removeHologram(id);
        }

        this.ourHolograms.remove(id);
    }

    @Override
    public void updateHologram(String id, List<String> lines) {
        Hologram hologram = DHAPI.getHologram(id);

        if (hologram != null) {
            DHAPI.setHologramLines(hologram, lines);
        }
    }

    @Override
    public void bulkUpdateHolograms(Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            updateHologram(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void removeAllHolograms() {
        for (String id : this.ourHolograms) {
            Hologram hologram = DHAPI.getHologram(id);

            if (hologram != null) {
                hologram.delete();
                DecentHologramsAPI.get().getHologramManager().removeHologram(id);
            }
        }

        this.ourHolograms.clear();
    }

    @Override
    public boolean isHologramLoaded(String id) {
        return DHAPI.getHologram(id) != null;
    }

    private void createAt(String id, Location location, List<String> lines) {
        location = fixLocation(location);

        if (DHAPI.getHologram(id) != null) {
            return;
        }

        DHAPI.createHologram(id, location, lines);
        this.ourHolograms.add(id);
    }
}
