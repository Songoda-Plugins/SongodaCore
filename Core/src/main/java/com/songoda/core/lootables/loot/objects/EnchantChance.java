package com.songoda.core.lootables.loot.objects;

import org.bukkit.enchantments.Enchantment;

public class EnchantChance {
    private final Enchantment enchantment;
    private final int level;
    private final double chanceOverride;

    public EnchantChance(Enchantment enchantment, int level, double chanceOverride) {
        this.enchantment = enchantment;
        this.level = level;
        this.chanceOverride = chanceOverride;
    }

    public Enchantment getEnchantment() {
        return this.enchantment;
    }

    public int getLevel() {
        return this.level;
    }

    public double getChanceOverride() {
        return this.chanceOverride;
    }
}
