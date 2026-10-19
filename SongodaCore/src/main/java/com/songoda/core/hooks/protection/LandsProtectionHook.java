package com.songoda.core.hooks.protection;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Lands role flag checks for block breaking and interaction.
 */
public final class LandsProtectionHook implements ProtectionHook {

    private final LandsIntegration lands;

    public LandsProtectionHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Lands");
        if (plugin == null) {
            throw new IllegalStateException("Lands is not available");
        }
        this.lands = LandsIntegration.of(plugin);
    }

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        return hasFlag(player, location, Flags.BLOCK_BREAK);
    }

    @Override
    public boolean canPlace(@NotNull Player player, @NotNull Location location) {
        return hasFlag(player, location, Flags.BLOCK_PLACE);
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        return hasFlag(player, location, Flags.INTERACT_CONTAINER);
    }

    private boolean hasFlag(Player player, Location location, RoleFlag flag) {
        Area area = this.lands.getArea(location);
        if (area == null || area.getRole(player.getUniqueId()) == null) {
            return true;
        }

        return area.getRole(player.getUniqueId()).hasFlag(flag);
    }
}
