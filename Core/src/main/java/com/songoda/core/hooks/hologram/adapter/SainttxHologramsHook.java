package com.songoda.core.hooks.hologram.adapter;

import com.songoda.core.hooks.hologram.HologramHook;
import com.sainttx.holograms.api.Hologram;
import com.sainttx.holograms.api.HologramManager;
import com.sainttx.holograms.api.HologramPlugin;
import com.sainttx.holograms.api.line.HologramLine;
import com.sainttx.holograms.api.line.TextLine;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SainttxHologramsHook extends HologramHook {
    private final ArrayList<String> ourHologramIds = new ArrayList<>(0);
    private String hologramNamePrefix;
    private HologramManager sainttxHologramManager;

    @Override
    public String getName() {
        return "SainttxHolograms";
    }

    @Override
    public @NotNull String[] getPluginDependencies() {
        return new String[] {"Holograms"};
    }

    @Override
    public void activate(Plugin plugin) {
        this.hologramNamePrefix = plugin.getClass().getName() + "-";
        this.sainttxHologramManager = JavaPlugin.getPlugin(HologramPlugin.class).getHologramManager();
    }

    @Override
    public void deactivate() {
        removeAll();
        this.hologramNamePrefix = null;
        this.sainttxHologramManager = null;
    }

    @Override
    public boolean exists(@NotNull String id) {
        return this.sainttxHologramManager.getHologram(getHologramName(id)) != null;
    }

    @Override
    public void create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        if (exists(id)) {
            throw new IllegalStateException("Cannot create hologram that already exists: " + getHologramName(id));
        }

        Hologram hologram = new Hologram(getHologramName(id), getNormalizedLocation(location), false);
        for (String line : lines) {
            hologram.addLine(createByReflection(hologram, line));
        }
        this.sainttxHologramManager.addActiveHologram(hologram);

        this.ourHologramIds.add(id);
    }

    @Override
    public void update(@NotNull String id, @NotNull List<String> lines) {
        Hologram hologram = this.sainttxHologramManager.getHologram(getHologramName(id));
        if (hologram == null) {
            throw new IllegalStateException("Cannot update hologram that does not exist: " + getHologramName(id));
        }

        for (HologramLine hologramLine : hologram.getLines().toArray(new HologramLine[0])) {
            hologram.removeLine(hologramLine);
        }
        for (String line : lines) {
            hologram.addLine(new TextLine(hologram, line));
        }
    }

    @Override
    public void updateBulk(@NotNull Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove(@Nullable String id) {
        removeSainttxHologramIfExists(getHologramName(id));
        this.ourHologramIds.remove(id);
    }

    @Override
    public void removeAll() {
        for (String id : this.ourHologramIds) {
            removeSainttxHologramIfExists(getHologramName(id));
        }
        this.ourHologramIds.clear();
        this.ourHologramIds.trimToSize();
    }

    @Override
    protected double getYOffset() {
        return 0.5;
    }

    private String getHologramName(String id) {
        if (this.hologramNamePrefix == null) {
            throw new IllegalStateException("Hook has not been activated yet");
        }
        return this.hologramNamePrefix + id;
    }

    private void removeSainttxHologramIfExists(String name) {
        Hologram hologram = this.sainttxHologramManager.getHologram(name);
        if (hologram != null) {
            this.sainttxHologramManager.deleteHologram(hologram);
        }
    }

    // FIXME: This is a workaround for the compile/JVM which causes calls to the constructor to
    //        loading the HologramLine when loading this class, which causes a NoClassDefFoundError
    //        when the HologramLine class is not available at runtime.
    //        Des ist basically ein Design-Problem des Hook-Interface, welches Hook-Infos/-Meta und Hook-Implementierung
    //        in einer Klasse vereint. Best-Case wäre dass die Hook-Implementierung nie geladen wird, bis sie tatsäclich gebraucht wird.
    private static HologramLine createByReflection(Hologram hologram, String line) {
        try {
            return TextLine.class.getConstructor(Hologram.class, String.class).newInstance(hologram, line);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
