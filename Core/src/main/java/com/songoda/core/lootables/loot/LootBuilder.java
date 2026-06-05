package com.songoda.core.lootables.loot;

import com.songoda.core.lootables.loot.objects.EnchantChance;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LootBuilder {
    private final Loot loot;

    public LootBuilder() {
        this.loot = new Loot();
    }

    public LootBuilder setMaterial(XMaterial material) {
        this.loot.setMaterial(material);
        return this;
    }

    public LootBuilder setName(String name) {
        this.loot.setName(name);
        return this;
    }

    public LootBuilder addLore(String... lore) {
        this.loot.setLore(Arrays.asList(lore));
        return this;
    }

    public LootBuilder addEnchants(Tuple<String, Integer>... tuples) {
        Map<String, Integer> enchants = new HashMap<>();

        for (Tuple<String, Integer> tuple : tuples) {
            enchants.put(tuple.getKey(), tuple.getValue());
        }

        this.loot.setEnchants(enchants);
        return this;
    }

    public LootBuilder addEnchantChances(EnchantChance... enchantChances) {
        Map<String, Double> enchants = new HashMap<>();

        for (EnchantChance chance : enchantChances) {
            enchants.put(chance.getEnchantment().getName() + ":" + chance.getLevel(), chance.getChanceOverride());
        }

        this.loot.setEnchantChances(enchants);
        return this;
    }

    public LootBuilder setBurnedMaterial(XMaterial material) {
        this.loot.setBurnedMaterial(material);
        return this;
    }

    public LootBuilder setChance(double chance) {
        this.loot.setChance(chance);
        return this;
    }

    public LootBuilder setMin(int min) {
        this.loot.setMin(min);
        return this;
    }

    public LootBuilder setMax(int max) {
        this.loot.setMax(max);
        return this;
    }

    public LootBuilder setDamageMin(int min) {
        this.loot.setDamageMin(min);
        return this;
    }

    public LootBuilder setDamageMax(int max) {
        this.loot.setDamageMax(max);
        return this;
    }

    public LootBuilder setAllowLootingEnchant(boolean allow) {
        this.loot.setAllowLootingEnchant(allow);
        return this;
    }

    public LootBuilder setLootingIncrease(double increase) {
        this.loot.setLootingIncrease(increase);
        return this;
    }

    public LootBuilder addOnlyDropFors(EntityType... types) {
        this.loot.addOnlyDropFor(types);
        return this;
    }

    public LootBuilder addChildLoot(Loot... loots) {
        this.loot.addChildLoots(loots);
        return this;
    }

    public LootBuilder setChildDropCount(int count) {
        this.loot.setChildDropCountMin(count);
        this.loot.setChildDropCountMax(count);

        return this;
    }

    public LootBuilder setChildDropCounMin(int count) {
        this.loot.setChildDropCountMin(count);
        return this;
    }

    public LootBuilder setChildDropCountMax(int count) {
        this.loot.setChildDropCountMax(count);
        return this;
    }

    public LootBuilder setRequireCharged(boolean require) {
        this.loot.setRequireCharged(require);
        return this;
    }

    public Loot build() {
        return this.loot;
    }

    public static class Tuple<key, value> {
        public final key x;
        public final value y;

        public Tuple(key x, value y) {
            this.x = x;
            this.y = y;
        }

        public key getKey() {
            return this.x;
        }

        public value getValue() {
            return this.y;
        }
    }
}
