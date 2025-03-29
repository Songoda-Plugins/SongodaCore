package com.songoda.core.hooks.protection;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import com.songoda.core.SongodaPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class RedProtectProtection extends Protection {
    private final RedProtectAPI api;

    public RedProtectProtection(Plugin plugin) {
        super(plugin);

        this.api = RedProtect.get().getAPI();
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        Region region = api.getRegion(location);

        if (region == null) {
            return true;
        }

        return region.canBuild(player);
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        Region region = api.getRegion(location);

        if (region == null) {
            return true;
        }

        return region.canBuild(player);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        Region region = api.getRegion(location);

        if (region == null) {
            return true;
        }

        return region.canChest(player);
    }

    @Override
    public String getName() {
        return "RedProtect";
    }

    @Override
    public boolean isEnabled() {
        return api != null;
    }
}
