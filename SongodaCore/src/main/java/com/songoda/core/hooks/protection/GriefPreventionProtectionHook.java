package com.songoda.core.hooks.protection;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * GriefPrevention claim checks for block breaking and interaction.
 */
public final class GriefPreventionProtectionHook implements ProtectionHook {

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
        return claim == null || claim.allowBreak(player, location.getBlock().getType()) == null;
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
        return claim == null || claim.allowAccess(player) == null;
    }
}
