package com.songoda.core.hooks.holograms;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import com.Zrips.CMI.Modules.Holograms.HologramManager;
import com.songoda.core.SongodaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class CMIHolograms extends Holograms {
    private static CMI cmi;
    private static HologramManager cmiHologramManager;
    private static HashSet<String> ourHolograms = new HashSet<>();
    private static Method cmi_CMIHologram_getLines;
    private static boolean useOldMethod;

    static {
        try {
            useOldMethod = CMIHologram.class.getDeclaredField("lines").getDeclaringClass() == String[].class;
            cmi_CMIHologram_getLines = CMIHologram.class.getMethod("getLines");
        } catch (NoSuchFieldException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public CMIHolograms(Plugin plugin) {
        super(plugin);

        cmi = (CMI) Bukkit.getPluginManager().getPlugin("CMI");

        if (cmi != null) {
            cmiHologramManager = cmi.getHologramManager();
        }
    }

    @Override
    public String getName() {
        return "CMI";
    }

    @Override
    public boolean isEnabled() {
        return cmi != null && cmi.isEnabled();
    }

    @Override
    protected double defaultHeightOffset() {
        return 1;
    }

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        createAt(id, fixLocation(location), lines);
    }

    @Override
    public void removeHologram(String id) {
        CMIHologram holo = cmiHologramManager.getByName(id);

        if (holo != null) {
            cmiHologramManager.removeHolo(holo);
        }

        ourHolograms.remove(id);
    }

    @Override
    public void removeAllHolograms() {
        for (String id : ourHolograms) {
            CMIHologram holo = cmiHologramManager.getByName(id);

            if (holo != null) {
                cmiHologramManager.removeHolo(holo);
            }
        }

        ourHolograms.clear();
    }

    @Override
    public boolean isHologramLoaded(String id) {
        return cmiHologramManager.getByName(id) != null;
    }

    @Override
    public void updateHologram(String id, List<String> lines) {
        CMIHologram holo = cmiHologramManager.getByName(id);

        if (holo != null) {
            // only update if there is a change to the text
            List<String> holoLines;
            try {
                if (useOldMethod) {
                    holoLines = Arrays.asList((String[]) cmi_CMIHologram_getLines.invoke(holo));
                } else {
                    holoLines = (List<String>) cmi_CMIHologram_getLines.invoke(holo);
                }
            } catch (Exception ex) {
                Logger.getLogger(CMIHolograms.class.getName()).log(Level.SEVERE, "CMI Hologram error!", ex);
                holoLines = Collections.emptyList();
            }

            boolean isChanged = lines.size() != holoLines.size();
            if (!isChanged) {
                // double-check the lines
                for (int i = 0; !isChanged && i < lines.size(); ++i) {
                    isChanged = !holo.getLine(i).equals(lines.get(i));
                }
            }

            if (isChanged) {
                holo.setLines(lines);
                holo.update();
            }

            return;
        }
    }

    @Override
    public void bulkUpdateHolograms(Map<String, List<String>> hologramData) {
        for (Map.Entry<String, List<String>> entry : hologramData.entrySet()) {
            updateHologram(entry.getKey(), entry.getValue());
        }
    }

    private void createAt(String id, Location location, List<String> lines) {
        CMIHologram holo = new CMIHologram(id, location);
        holo.setLines(lines);

        cmiHologramManager.addHologram(holo);
        holo.update();

        ourHolograms.add(id);
    }
}
