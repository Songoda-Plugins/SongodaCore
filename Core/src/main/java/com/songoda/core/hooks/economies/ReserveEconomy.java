package com.songoda.core.hooks.economies;

import com.songoda.core.SongodaPlugin;
import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

/**
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class ReserveEconomy extends Economy {
    EconomyAPI economyAPI;

    public ReserveEconomy() {
        if (Reserve.instance().economyProvided()) {
            economyAPI = Reserve.instance().economy();
        }
    }

    @Override
    public boolean isEnabled() {
        return Reserve.instance().isEnabled();
    }

    @Override
    public String getName() {
        return "Reserve";
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economyAPI.getBankHoldings(player.getUniqueId()).doubleValue();
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        return economyAPI.hasHoldings(player.getUniqueId(), BigDecimal.valueOf(cost));
    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        return economyAPI.removeHoldings(player.getUniqueId(), BigDecimal.valueOf(cost));
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return economyAPI.addHoldings(player.getUniqueId(), BigDecimal.valueOf(amount));
    }
}
