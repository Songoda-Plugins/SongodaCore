package com.songoda.core.hooks.plugin.vault;

import net.milkbowl.vault.economy.Economy;
import com.songoda.core.SongodaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;
import java.util.function.Consumer;

public class EconomyHook {

    private static EconomyWrapper economy;

    public static void init() {
        //Get service from bukkit
        try {
            RegisteredServiceProvider<Economy> economyService = SongodaPlugin.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
            economy = new EconomyWrapper(economyService.getProvider());
        } catch (Throwable e) {
            SongodaPlugin.getInstance().getLogger().warning("Vault not found! Economy features will not work.");
        }
    }

    public static Optional<EconomyWrapper> getEconomy() {
        return Optional.ofNullable(economy);
    }

    public static void access(Consumer<EconomyWrapper> economyConsumer) {
        if (economy == null) {
            return;
        }
        economyConsumer.accept(economy);
    }
}
