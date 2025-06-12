package com.songoda.core.hooks.economies;

import com.songoda.core.SongodaPlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class PlayerPointsEconomy extends Economy {
    private final PlayerPoints playerPoints;

    public PlayerPointsEconomy() {
        this.playerPoints = (PlayerPoints) Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
    }

    private int convertAmount(double amount) {
        return (int) Math.ceil(amount);
    }

    @Override
    public boolean isEnabled() {
        return playerPoints.isEnabled();
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return playerPoints.getAPI().look(player.getUniqueId());
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        int amount = convertAmount(cost);

        return playerPoints.getAPI().look(player.getUniqueId()) >= amount;
    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        int amount = convertAmount(cost);

        return playerPoints.getAPI().take(player.getUniqueId(), amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        int amt = convertAmount(amount);

        return playerPoints.getAPI().give(player.getUniqueId(), amt);
    }
}
