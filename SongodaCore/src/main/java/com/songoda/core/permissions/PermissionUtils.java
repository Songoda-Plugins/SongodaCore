package com.songoda.core.permissions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities for resolving numeric limits granted through player permissions.
 */
public final class PermissionUtils {

    private PermissionUtils() {
    }

    /**
     * Returns the greatest non-negative integer granted using the supplied permission prefix.
     *
     * <p>When LuckPerms is enabled, its cached permission data is used so inherited and
     * context-sensitive permissions are included. Otherwise, the player's effective Bukkit
     * permissions are scanned.</p>
     *
     * @param player player whose permissions are checked
     * @param permissionPrefix prefix preceding the numeric limit
     * @param fallbackValue value returned when no numeric limit is granted, or when the exact
     *                      wildcard permission is granted
     * @return the greatest granted limit, or {@code fallbackValue}
     */
    public static int getHighestPermissionValue(
            @NotNull Player player,
            @NotNull String permissionPrefix,
            int fallbackValue) {
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            Integer luckPermsValue = LuckPermsPermissionProvider.getHighestPermissionValue(
                    player,
                    permissionPrefix,
                    fallbackValue);
            if (luckPermsValue != null) {
                return luckPermsValue;
            }
        }

        int highest = 0;
        boolean found = false;
        for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
            if (!permissionInfo.getValue()) {
                continue;
            }

            String permission = permissionInfo.getPermission();
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
