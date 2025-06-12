package com.songoda.core.hooks.hologram.adapter;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import com.Zrips.CMI.Modules.Holograms.HologramManager;
import com.songoda.core.hooks.hologram.HologramHook;
import net.Zrips.CMILib.Container.CMILocation;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CmiHologramHook extends HologramHook {
    private static final String CMI_PLUGIN_NAME = "CMI";

    private final ArrayList<String> ourHologramIds = new ArrayList<>(0);
    private String hologramNamePrefix;

    private HologramManager cmiHologramManager;

    @Override
    public String getName() {
        return CMI_PLUGIN_NAME;
    }

    @Override
    public @NotNull String[] getPluginDependencies() {
        return new String[] {CMI_PLUGIN_NAME};
    }

    @Override
    public void activate(Plugin plugin) {
        this.hologramNamePrefix = plugin.getClass().getName() + "-";
        this.cmiHologramManager = JavaPlugin.getPlugin(CMI.class).getHologramManager();
    }

    @Override
    public void deactivate() {
        removeAll();
        this.hologramNamePrefix = null;
        this.cmiHologramManager = null;
    }

    @Override
    public boolean exists(@NotNull String id) {
        return this.cmiHologramManager.getByName(getHologramName(id)) != null;
    }

    @Override
    public void create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        if (exists(id)) {
            throw new IllegalStateException("Cannot create hologram that already exists: " + getHologramName(id));
        }

        CMIHologram hologram = new CMIHologram(getHologramName(id), new CMILocation(getNormalizedLocation(location)));
        hologram.setLines(lines);
        this.cmiHologramManager.addHologram(hologram);
        hologram.update();

        this.ourHologramIds.add(id);
    }

    @Override
    public void update(@NotNull String id, @NotNull List<String> lines) {
        CMIHologram hologram = this.cmiHologramManager.getByName(getHologramName(id));
        if (hologram == null) {
            throw new IllegalStateException("Cannot update hologram that does not exist: " + getHologramName(id));
        }

        hologram.setLines(lines);
        hologram.update();
    }

    @Override
    public void updateBulk(@NotNull Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove(@Nullable String id) {
        CMIHologram hologram = this.cmiHologramManager.getByName(getHologramName(id));
        if (hologram != null) {
            this.cmiHologramManager.removeHolo(hologram);
        }

        this.ourHologramIds.remove(id);
    }

    @Override
    public void removeAll() {
        for (String id : this.ourHologramIds) {
            CMIHologram hologram = this.cmiHologramManager.getByName(getHologramName(id));
            if (hologram != null) {
                this.cmiHologramManager.removeHolo(hologram);
            }
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
