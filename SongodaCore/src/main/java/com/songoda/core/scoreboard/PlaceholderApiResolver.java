package com.songoda.core.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Optional PlaceholderAPI adapter used by configured scoreboards.
 */
final class PlaceholderApiResolver {

    private static volatile Method setPlaceholders;

    private PlaceholderApiResolver() {
    }

    static boolean isAvailable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }
        try {
            Method method = setPlaceholders;
            if (method == null) {
                method = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                        .getMethod("setPlaceholders", Player.class, String.class);
                setPlaceholders = method;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static @NotNull String resolve(@NotNull Player player, @NotNull String placeholder) {
        try {
            Method method = setPlaceholders;
            if (method == null) {
                if (!isAvailable()) {
                    return placeholder;
                }
                method = setPlaceholders;
            }
            Object result = method.invoke(null, player, placeholder);
            return result == null ? placeholder : result.toString();
        } catch (ReflectiveOperationException ignored) {
            return placeholder;
        }
    }
}
