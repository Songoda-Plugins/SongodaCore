package com.songoda.core.hooks.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * BentoBox island protection checks for block breaking and interaction.
 */
public final class BentoBoxProtectionHook implements ProtectionHook {

    private final IslandsManager islandsManager;

    public BentoBoxProtectionHook() {
        this.islandsManager = BentoBox.getInstance().getIslands();
    }

    @Override
    public boolean canBreak(@NotNull Player player, @NotNull Location location) {
        return hasPermission(player, location, Flags.BREAK_BLOCKS);
    }

    @Override
    public boolean canInteract(@NotNull Player player, @NotNull Location location) {
        return hasPermission(player, location, Flags.CONTAINER);
    }

    private boolean hasPermission(Player player, Location location, Flag flag) {
        if (!BentoBox.getInstance().getIWM().inWorld(location)) {
            return true;
        }

        Island island = this.islandsManager.getIslandAt(location).orElse(null);
        if (island == null) {
            return flag.isSetForWorld(location.getWorld());
        }
        return island.isAllowed(User.getInstance(player), flag);
    }
}
