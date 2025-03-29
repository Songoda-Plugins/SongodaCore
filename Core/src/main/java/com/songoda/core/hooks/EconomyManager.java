package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.economies.Economy;
import com.songoda.core.utils.NumberUtils;
import org.bukkit.OfflinePlayer;

/**
 * A convenience class for static access to an Economy HookManager
 *
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class EconomyManager {
    private static char currencySymbol = '$';

    private static final HookManager<Economy> manager = new HookManager(Economy.class);

    /**
     * Load all supported economy plugins. <br />
     * Note: This method should be called in your plugin's onEnable() section
     */
    public static void load() {
        manager.load();
    }

    public static HookManager getManager() {
        return manager;
    }

    /**
     * Grab the default economy plugin. <br />
     * NOTE: using a default economy assumes that this library is shaded
     *
     * @return returns null if no plugin enabled
     */
    public static Economy getEconomy() {
        return manager.getCurrentHook();
    }

    /**
     * Check to see if there is a default economy loaded. <br />
     * NOTE: using a default economy assumes that this library is shaded
     *
     * @return returns false if there are no supported economy plugins
     */
    public static boolean isEnabled() {
        return manager.isEnabled();
    }

    /**
     * Get the name of the economy plugin being used. <br />
     * NOTE: using a default economy assumes that this library is shaded
     */
    public static String getName() {
        return manager.getName();
    }

    /**
     * Format the given amount to a human-readable string in this currency
     *
     * @param amt amount to display
     *
     * @return a currency string as formatted by the economy plugin
     */
    public static String formatEconomy(double amt) {
        return NumberUtils.formatEconomy(currencySymbol, amt);
    }

    /**
     * Get the players available balance
     *
     * @param player player
     *
     * @return the amount of available balance
     */
    public static double getBalance(OfflinePlayer player) {
        if (!manager.isEnabled())
            return 0;
        return manager.getCurrentHook().getBalance(player);
    }

    /**
     * Check to see if a player has at least some balance available. <br />
     * NOTE: using a default economy assumes that this library is shaded
     *
     * @param player player to check
     * @param cost   minimum amount this player should have
     *
     * @return true if this player can have this amount withdrawn
     */
    public static boolean hasBalance(OfflinePlayer player, double cost) {
        return manager.isEnabled() && manager.getCurrentHook().hasBalance(player, cost);
    }

    /**
     * Try to withdraw an amount from a player's balance. <br />
     * NOTE: using a default economy assumes that this library is shaded
     *
     * @param player player to check
     * @param cost   amount to remove from this player
     *
     * @return true if the total amount was withdrawn successfully
     */
    public static boolean withdrawBalance(OfflinePlayer player, double cost) {
        return manager.isEnabled() && manager.getCurrentHook().withdrawBalance(player, cost);
    }

    /**
     * Try to add an amount to a player's balance. <br />
     * NOTE: using a default economy assumes that this library is shaded
     *
     * @param player player to check
     * @param amount amount to add to this player
     *
     * @return true if the total amount was added successfully
     */
    public static boolean deposit(OfflinePlayer player, double amount) {
        return manager.isEnabled() && manager.getCurrentHook().deposit(player, amount);
    }

    /**
     * Change the curency symbol used in the #formatEconomy method.
     *
     * @param currencySymbol the new symbol
     */
    public static void setCurrencySymbol(String currencySymbol) {
        EconomyManager.currencySymbol = currencySymbol.charAt(0);
    }

    /**
     * Change the curency symbol used in the #formatEconomy method.
     *
     * @param currencySymbol the new symbol
     */
    public static void setCurrencySymbol(char currencySymbol) {
        EconomyManager.currencySymbol = currencySymbol;
    }
}
