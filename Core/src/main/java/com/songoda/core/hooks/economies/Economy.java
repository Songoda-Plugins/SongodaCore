package com.songoda.core.hooks.economies;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.OutdatedHookInterface;
import org.bukkit.OfflinePlayer;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public abstract class Economy implements OutdatedHookInterface {
    /**
     * Get the players available balance
     *
     * @param player player
     *
     * @return the amount of available balance
     */
    public abstract double getBalance(OfflinePlayer player);

    /**
     * Check to see if a player has at least some balance available
     *
     * @param player player to check
     * @param cost   minimum amount this player should have
     *
     * @return true if this player can have this amount withdrawn
     */
    public abstract boolean hasBalance(OfflinePlayer player, double cost);

    /**
     * Try to withdraw an amount from a player's balance
     *
     * @param player player to check
     * @param cost   amount to remove from this player
     *
     * @return true if the total amount was withdrawn successfully
     */
    public abstract boolean withdrawBalance(OfflinePlayer player, double cost);

    /**
     * Try to add an amount to a player's balance
     *
     * @param player player to check
     * @param amount amount to add to this player
     *
     * @return true if the total amount was added successfully
     */
    public abstract boolean deposit(OfflinePlayer player, double amount);
}
