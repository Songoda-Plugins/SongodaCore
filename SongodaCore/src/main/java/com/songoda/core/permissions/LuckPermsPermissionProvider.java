package com.songoda.core.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

final class LuckPermsPermissionProvider {

    private LuckPermsPermissionProvider() {
    }

    @Nullable
    static Integer getHighestPermissionValue(
            @NotNull Player player,
            @NotNull String permissionPrefix,
            int fallbackValue) {
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            return null;
        }

        PlayerAdapter<Player> playerAdapter = luckPerms.getPlayerAdapter(Player.class);
        CachedPermissionData permissionData = playerAdapter.getPermissionData(player);
        Map<String, Boolean> permissions = permissionData.getPermissionMap();

        int highest = 0;
        boolean found = false;
        for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
            if (!permissionEntry.getValue()) {
                continue;
            }

            String permission = permissionEntry.getKey();
            if (!permission.startsWith(permissionPrefix)) {
                continue;
            }

            String value = permission.substring(permissionPrefix.length());
            if (value.equals("*")) {
                return fallbackValue;
            }

            try {
                int limit = Integer.parseInt(value);
                if (limit >= highest) {
                    highest = limit;
                    found = true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return found ? highest : fallbackValue;
    }
}
