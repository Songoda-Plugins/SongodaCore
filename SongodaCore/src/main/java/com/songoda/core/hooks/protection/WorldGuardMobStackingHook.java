package com.songoda.core.hooks.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Isolates optional WorldGuard API types from consumers of mob stacking protections.
 */
public final class WorldGuardMobStackingHook {

    private static final String FLAG_NAME = "mob-stacking";

    private WorldGuardMobStackingHook() {
    }

    /**
     * Registers or reuses the stacking flag before WorldGuard locks its registry.
     */
    public static void registerFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        Flag<?> existing = registry.get(FLAG_NAME);
        if (existing instanceof StateFlag) {
            return;
        }
        if (existing != null) {
            throw new IllegalStateException("WorldGuard mob-stacking flag has an incompatible type");
        }
        try {
            registry.register(new StateFlag(FLAG_NAME, true));
        } catch (FlagConflictException exception) {
            if (!(registry.get(FLAG_NAME) instanceof StateFlag)) {
                throw new IllegalStateException("WorldGuard mob-stacking flag has an incompatible type", exception);
            }
        }
    }

    /**
     * Evaluates the stacking flag for the supplied location.
     *
     * @param location the owned entity location
     * @return true unless the applicable region flag denies stacking
     */
    public static boolean isAllowed(@NotNull Location location) {
        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(FLAG_NAME);
        if (!(flag instanceof StateFlag stateFlag)) {
            return false;
        }
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .testState(BukkitAdapter.adapt(location), null, stateFlag);
    }
}
