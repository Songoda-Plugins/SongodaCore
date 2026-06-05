package com.songoda.core.hooks.protection;

import com.songoda.core.SongodaPlugin;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class PlotSquaredProtection extends Protection {
    public PlotSquaredProtection(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "PlotSquared";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlotSquared");
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return isPlayerAddedAtPlotLocation(player, location);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return isPlayerAddedAtPlotLocation(player, location);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return isPlayerAddedAtPlotLocation(player, location);
    }

    private boolean isPlayerAddedAtPlotLocation(Player player, Location location) {
        PlotArea plotArea = getApplicablePlotArea(location);
        if (plotArea == null) {
            return true;
        }

        for (Plot p : plotArea.getPlots()) {
            if (p.isAdded(player.getUniqueId())) {
                return true;
            }
        }

        return false;
    }

    private PlotArea getApplicablePlotArea(Location location) {
        return PlotSquared.get()
                .getPlotAreaManager()
                .getApplicablePlotArea(getPlotSquaredLocation(location));
    }

    private com.plotsquared.core.location.Location getPlotSquaredLocation(Location location) {
        return com.plotsquared.core.location.Location.at(
                location.getWorld().getName(),
                (int) location.getX(),
                (int) location.getY(),
                (int) location.getZ()
        );
    }
}
