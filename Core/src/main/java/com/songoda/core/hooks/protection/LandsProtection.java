package com.songoda.core.hooks.protection;

import com.songoda.core.SongodaPlugin;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class LandsProtection extends Protection {
    private final LandsIntegration landsIntegration;

    public LandsProtection(Plugin plugin) {
        super(plugin);

        this.landsIntegration = LandsIntegration.of(plugin);
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return hasPerms(player, location, Flags.BLOCK_PLACE);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return hasPerms(player, location, Flags.BLOCK_BREAK);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return hasPerms(player, location, Flags.INTERACT_CONTAINER);
    }

    private boolean hasPerms(Player player, Location location, RoleFlag roleFlag) {
        Area area = this.landsIntegration.getArea(location);
        if (area == null) {
            return true;
        }

        return area.getRole(player.getUniqueId()).hasFlag(roleFlag);
    }

    @Override
    public String getName() {
        return "Lands";
    }

    @Override
    public boolean isEnabled() {
        return this.landsIntegration != null;
    }
}
