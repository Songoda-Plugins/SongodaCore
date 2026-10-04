package com.songoda.core.hooks.protection;

import com.songoda.ultimateclaims.api.UltimateClaimsApi;
import com.songoda.ultimateclaims.api.claim.ClaimPermission;
import com.songoda.ultimateclaims.api.claim.ClaimsService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * UltimateClaims protection checks for block breaking and interaction.
 */
public final class UltimateClaimsProtectionHook implements ProtectionHook {

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        ClaimsService claimsService = UltimateClaimsApi.getClaimsService();
        return claimsService != null && claimsService.hasPermission(player, location.getChunk(), ClaimPermission.BREAK.getKey());
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        ClaimsService claimsService = UltimateClaimsApi.getClaimsService();
        return claimsService != null && claimsService.hasPermission(player, location.getChunk(), ClaimPermission.INTERACT.getKey());
    }
}
