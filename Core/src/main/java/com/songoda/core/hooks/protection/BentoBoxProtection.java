package com.songoda.core.hooks.protection;

import com.songoda.core.SongodaPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandsManager;

import java.util.Optional;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class BentoBoxProtection extends Protection {
    private final IslandsManager islandsManager;

    public BentoBoxProtection(Plugin plugin) {
        super(plugin);

        this.islandsManager = BentoBox.getInstance().getIslands();
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        return hasPerms(player, location, Flags.PLACE_BLOCKS);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return hasPerms(player, location, Flags.BREAK_BLOCKS);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return hasPerms(player, location, Flags.CONTAINER);
    }

    private boolean hasPerms(Player player, Location location, Flag flag) {
        if (!BentoBox.getInstance().getIWM().inWorld(location)) {
            return true;
        }

        Optional<Island> optional = islandsManager.getIslandAt(location);
        if (!optional.isPresent()) {
            return flag.isSetForWorld(location.getWorld());
        }

        Island island = optional.get();
        User user = User.getInstance(player);

        return island.isAllowed(user, flag);
    }

    @Override
    public String getName() {
        return "BentoBox";
    }

    @Override
    public boolean isEnabled() {
        return islandsManager != null;
    }
}
