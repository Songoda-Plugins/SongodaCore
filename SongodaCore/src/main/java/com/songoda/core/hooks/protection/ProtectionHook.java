package com.songoda.core.hooks.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Protection provider for a region or claim plugin.
 */
public interface ProtectionHook {

    boolean canBreak(@NotNull Player player, @NotNull Location location);

    boolean canInteract(@NotNull Player player, @NotNull Location location);
}
