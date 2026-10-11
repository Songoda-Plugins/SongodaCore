package com.songoda.core.hooks.protection;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Residence permission checks for block breaking and interaction.
 */
public final class ResidenceProtectionHook implements ProtectionHook {

    private final Residence residence;

    public ResidenceProtectionHook() {
        this.residence = Residence.getInstance();
    }

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        ResidencePlayer residencePlayer = this.residence.getPlayerManager().getResidencePlayer(player);
        return residencePlayer.canBreakBlock(location.getBlock(), false);
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        if (this.residence.isDisabledWorldListener(location.getWorld()) || this.residence.isResAdminOn(player)) {
            return true;
        }

        FlagPermissions permissions = this.residence.getPermsByLocForPlayer(location, player);
        return permissions.playerHas(player, Flags.use, true);
    }
}
