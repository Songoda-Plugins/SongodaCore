package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.worldguard.WorldGuardFlagHandler;
import com.songoda.core.hooks.worldguard.WorldGuardRegionHandler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class WorldGuardHook {
    static boolean canHook = false;

    static {
        try {
            // if this class exists, we're good to use WG classes
            Class.forName("com.sk89q.worldguard.protection.flags.Flag");
            canHook = true;
        } catch (ClassNotFoundException ignore) {
        }
    }

    /**
     * Attempt to register a worldGuard flag (ALLOW/DENY) <br />
     * Note: This must be called before WorldGuard loads, or it will fail.
     *
     * @param flag  name of the flag to set
     * @param state default value of the flag
     */
    public static void addHook(@NotNull String flag, boolean state) {
        if (canHook) {
            WorldGuardFlagHandler.addHook(flag, state);
        }
    }

    /**
     * Check to see if WorldGuard is installed and hooked
     *
     * @return true if and only if WorldGuard exists and addHook() has been
     *         called and added successfully
     */
    public static boolean isEnabled() {
        return canHook && WorldGuardFlagHandler.isEnabled();
    }

    /**
     * Checks this location to see what this flag is set to
     *
     * @param l    location to check
     * @param flag ALLOW/DENY flag to check
     *
     * @return flag state, or null if undefined
     */
    @Nullable
    public static Boolean getBooleanFlag(@NotNull Location l, @NotNull String flag) {
        return canHook ? WorldGuardFlagHandler.getBooleanFlag(l, flag) : null;
    }

    /**
     * Query all regions that are in or intersect this chunk
     *
     * @param c    chunk to check for regions in
     * @param flag ALLOW/DENY flag to check
     *
     * @return flag state, or null if undefined
     */
    @Nullable
    public static Boolean getBooleanFlag(@NotNull Chunk c, @NotNull String flag) {
        return canHook ? WorldGuardFlagHandler.getBooleanFlag(c, flag) : null;
    }

    /**
     * Check to see if the pvp flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the pvp flag is not set for this region, or is set to DENY
     */
    public static boolean isPvpAllowed(@NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "pvp"), Boolean.TRUE);
    }

    /**
     * Check to see if the block-break flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the block-break flag is not set for this region, or is set to DENY
     */
    public static boolean isBreakAllowed(@NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "block-break"), Boolean.TRUE);
    }

    /**
     * Check to see if the build flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the build flag is not set for this region, or is set to DENY
     */
    public static boolean isBuildAllowed(@NotNull Player player, @NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "build", player), Boolean.TRUE);
    }

    /**
     * Check to see if the use flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the use flag is not set for this region, or is set to DENY
     */
    public static boolean isInteractAllowed(@NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "use"), Boolean.TRUE);
    }

    /**
     * Check to see if the other-explosion flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the other-explosion flag is not set for this region, or is set to DENY
     */
    public static boolean isExplosionsAllowed(@NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "other-explosion"), Boolean.TRUE);
    }

    /**
     * Check to see if the mob-spawning flag is set and is set to ALLOW
     *
     * @param loc Location to check
     *
     * @return false if the mob-spawning flag is not set for this region, or is set to DENY
     */
    public static boolean isMobSpawningAllowed(@NotNull Location loc) {
        return canHook && Objects.equals(WorldGuardFlagHandler.getBooleanFlag(loc, "mob-spawning"), Boolean.TRUE);
    }

    /**
     * @param loc Location to check
     *
     * @return A list of regions that contain this location.
     */
    @NotNull
    public static List<String> getRegionNames(@NotNull Location loc) {
        return canHook ? WorldGuardRegionHandler.getRegionNames(loc) : Collections.emptyList();
    }

    /**
     * @param c Chunk to check
     *
     * @return A list of regions that contain any part of this chunk.
     */
    @NotNull
    public static List<String> getRegionNames(@NotNull Chunk c) {
        return canHook ? WorldGuardRegionHandler.getRegionNames(c) : Collections.emptyList();
    }
}
