package com.songoda.core.hooks.protection;

import com.songoda.core.SongodaPlugin;
import com.songoda.ultimateclaims.UltimateClaims;
import com.songoda.ultimateclaims.claim.Claim;
import com.songoda.ultimateclaims.member.ClaimPerm;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class UltimateClaimsProtection extends Protection {
    private final UltimateClaims instance;

    public UltimateClaimsProtection(Plugin plugin) {
        super(plugin);

        this.instance = JavaPlugin.getPlugin(UltimateClaims.class);
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return hasPerms(player, location, ClaimPerm.PLACE);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return hasPerms(player, location, ClaimPerm.BREAK);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return hasPerms(player, location, ClaimPerm.INTERACT);
    }

    private boolean hasPerms(Player player, Location location, ClaimPerm claimPerm) {
        Claim claim = this.instance.getClaimManager().getClaim(location.getChunk());
        if (claim == null) {
            return true;
        }

        return claim.playerHasPerms(player, claimPerm);
    }

    @Override
    public String getName() {
        return "UltimateClaims";
    }

    @Override
    public boolean isEnabled() {
        return this.instance != null;
    }
}
