package com.songoda.core.hooks.protection;

import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * RedProtect region checks for block breaking and interaction.
 */
public final class RedProtectProtectionHook implements ProtectionHook {

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        Region region = RedProtect.get().getAPI().getRegion(location);
        if (region == null) {
            return true;
        }

        return region.canBuild(player);
    }

    @Override
    public boolean canPlace(@NotNull Player player, @NotNull Location location) {
        Region region = RedProtect.get().getAPI().getRegion(location);
        if (region == null) {
            return true;
        }

        return region.canBuild(player);
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        Region region = RedProtect.get().getAPI().getRegion(location);
        return region == null || region.canChest(player);
    }
}
