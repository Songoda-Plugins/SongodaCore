package com.songoda.core.hooks.protection;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.songoda.core.SongodaPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class ResidenceProtection extends Protection {
    private final Residence instance;

    public ResidenceProtection(Plugin plugin) {
        super(plugin);

        this.instance = Residence.getInstance();
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(player);

        return rPlayer.canPlaceBlock(location.getBlock(), false);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(player);

        return rPlayer.canBreakBlock(location.getBlock(), false);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return hasPerms(player, location, Flags.use);
    }

    private boolean hasPerms(Player player, Location location, Flags flag) {
        if (instance.isDisabledWorldListener(location.getWorld())) {
            return true;
        }

        if (instance.isResAdminOn(player)) {
            return true;
        }

        FlagPermissions perms = instance.getPermsByLocForPlayer(location, player);
        return perms.playerHas(player, flag, true);
    }

    @Override
    public String getName() {
        return "Residence";
    }

    @Override
    public boolean isEnabled() {
        return instance != null;
    }
}
