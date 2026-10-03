package com.songoda.core.hooks.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * WorldGuard region checks for block breaking and interaction.
 */
public final class WorldGuardProtectionHook implements ProtectionHook {

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                Flags.BLOCK_BREAK
        );
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                Flags.INTERACT
        );
    }
}
