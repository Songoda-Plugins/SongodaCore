package com.songoda.core.hooks.plugin.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class EconomyWrapper {

    private Economy economy;

    public EconomyWrapper(Economy economy) {
        this.economy = economy;
    }

    public boolean isEnabled() {
        return economy.isEnabled();
    }

    public String currencyNameSingular() {
        return economy.currencyNameSingular();
    }

    @Deprecated
    public EconomyResponse isBankOwner(String s, String s1) {
        return economy.isBankOwner(s, s1);
    }

    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        return economy.getBalance(offlinePlayer, s);
    }

    @Deprecated
    public EconomyResponse createBank(String s, String s1) {
        return economy.createBank(s, s1);
    }

    public String currencyNamePlural() {
        return economy.currencyNamePlural();
    }

    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
        return economy.withdrawPlayer(offlinePlayer, v);
    }

    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        return economy.createPlayerAccount(offlinePlayer, s);
    }

    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return economy.isBankOwner(s, offlinePlayer);
    }

    public String format(double v) {
        return economy.format(v);
    }

    @Deprecated
    public double getBalance(String s, String s1) {
        return economy.getBalance(s, s1);
    }

    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return economy.createBank(s, offlinePlayer);
    }

    @Deprecated
    public EconomyResponse withdrawPlayer(String s, double v) {
        return economy.withdrawPlayer(s, v);
    }

    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return economy.withdrawPlayer(offlinePlayer, s, v);
    }

    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return economy.createPlayerAccount(offlinePlayer);
    }

    public EconomyResponse bankDeposit(String s, double v) {
        return economy.bankDeposit(s, v);
    }

    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        return economy.hasAccount(offlinePlayer);
    }

    @Deprecated
    public boolean has(String s, double v) {
        return economy.has(s, v);
    }

    @Deprecated
    public boolean createPlayerAccount(String s, String s1) {
        return economy.createPlayerAccount(s, s1);
    }

    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return economy.depositPlayer(offlinePlayer, s, v);
    }

    @Deprecated
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        return economy.withdrawPlayer(s, s1, v);
    }

    @Deprecated
    public boolean hasAccount(String s) {
        return economy.hasAccount(s);
    }

    @Deprecated
    public EconomyResponse depositPlayer(String s, double v) {
        return economy.depositPlayer(s, v);
    }

    public EconomyResponse bankHas(String s, double v) {
        return economy.bankHas(s, v);
    }

    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        return economy.hasAccount(offlinePlayer, s);
    }

    @Deprecated
    public boolean has(String s, String s1, double v) {
        return economy.has(s, s1, v);
    }

    public List<String> getBanks() {
        return economy.getBanks();
    }

    @Deprecated
    public boolean createPlayerAccount(String s) {
        return economy.createPlayerAccount(s);
    }

    @Deprecated
    public boolean hasAccount(String s, String s1) {
        return economy.hasAccount(s, s1);
    }

    @Deprecated
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return economy.depositPlayer(s, s1, v);
    }

    public boolean has(OfflinePlayer offlinePlayer, double v) {
        return economy.has(offlinePlayer, v);
    }

    public EconomyResponse bankWithdraw(String s, double v) {
        return economy.bankWithdraw(s, v);
    }

    public int fractionalDigits() {
        return economy.fractionalDigits();
    }

    public double getBalance(OfflinePlayer offlinePlayer) {
        return economy.getBalance(offlinePlayer);
    }

    @Deprecated
    public EconomyResponse isBankMember(String s, String s1) {
        return economy.isBankMember(s, s1);
    }

    public EconomyResponse deleteBank(String s) {
        return economy.deleteBank(s);
    }

    public boolean hasBankSupport() {
        return economy.hasBankSupport();
    }

    @Deprecated
    public double getBalance(String s) {
        return economy.getBalance(s);
    }

    public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
        return economy.has(offlinePlayer, s, v);
    }

    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
        return economy.depositPlayer(offlinePlayer, v);
    }

    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return economy.isBankMember(s, offlinePlayer);
    }

    public EconomyResponse bankBalance(String s) {
        return economy.bankBalance(s);
    }

    public String getName() {
        return economy.getName();
    }
}
