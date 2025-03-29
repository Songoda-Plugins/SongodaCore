package com.songoda.core.lootables.loot;

import com.songoda.core.nms.Nms;
import com.songoda.core.utils.TextUtils;
import com.cryptomorin.xseries.XMaterial;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Loot {
    // Command ran for this drop.
    @SerializedName("Command")
    private String command;

    // Xp for this drop.
    @SerializedName("xp")
    private int xp = 0;

    // Material used for this drop.
    @SerializedName("Type")
    private XMaterial material;

    // The override for the item name.
    @SerializedName("Name")
    private String name = null;

    // The override for the item lore.
    @SerializedName("Lore")
    private List<String> lore = null;

    // The override for the item enchantments.
    @SerializedName("Enchantments")
    private Map<String, Integer> enchants = null;

    // Material used if entity died on fire.
    @SerializedName("Burned Type")
    private XMaterial burnedMaterial = null;

    // Chance that this drop will take place.
    @SerializedName("Chance")
    private double chance = 100;

    // Minimum amount of this item.
    @SerializedName("Min")
    private int min = 1;

    // Maximum amount of this item.
    @SerializedName("Max")
    private int max = 1;

    // The override for chances applied by the wield item.
    @SerializedName("Wielded Enchantment Chance Overrides")
    private Map<String, Double> enchantChances = null;

    // Min amount of applied damage.
    @SerializedName("Damage Min")
    private Integer damageMin = null;

    // Max amount of applied damage.
    @SerializedName("Damage Max")
    private Integer damageMax = null;

    // Will the looting enchantment be usable for this loot?
    @SerializedName("Looting")
    private boolean allowLootingEnchant = true;

    // The looting chance increase.
    @SerializedName("Looting Chance Increase")
    private Double lootingIncrease;

    // Should this drop only be applicable for specific entities?
    @SerializedName("Only Drop For")
    private List<EntityType> onlyDropFor;

    // How many child loots should drop?
    @SerializedName("Child Loot Drop Count Min")
    private Integer childDropCountMin;
    @SerializedName("Child Loot Drop Count Max")
    private Integer childDropCountMax;

    // Should this drop house child drops?
    @SerializedName("Child Loot")
    private List<Loot> childLoot;

    // Should the entity be charged? (Only works on creepers)
    private boolean requireCharged = false;

    public XMaterial getMaterial() {
        return this.material;
    }

    public void setMaterial(XMaterial material) {
        this.material = material;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getXp() {
        return this.xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getName() {
        return TextUtils.formatText(this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        if (this.lore == null) {
            return null;
        }

        List<String> lore = new ArrayList<>();

        for (String line : this.lore) {
            lore.add(TextUtils.formatText(line));
        }

        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
    }

    public ItemStack getEnchants(ItemStack item) {
        if (this.enchants == null) {
            return null;
        }

        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            for (Map.Entry<String, Integer> entry : this.enchants.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }

                if (entry.getKey().equalsIgnoreCase("RANDOM")) {
                    item = Nms.getImplementations().getItem().copyAndApplyRandomEnchantment(item, entry.getValue());
                    continue;
                }

                meta.addStoredEnchant(Enchantment.getByName(entry.getKey()), entry.getValue(), true);
            }
            item.setItemMeta(meta);
            return item;
        }

        Map<Enchantment, Integer> enchants = new HashMap<>();
        for (Map.Entry<String, Integer> entry : this.enchants.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (entry.getKey().equalsIgnoreCase("RANDOM")) {
                item = Nms.getImplementations().getItem().copyAndApplyRandomEnchantment(item, entry.getValue());
                continue;
            }

            enchants.put(Enchantment.getByName(entry.getKey()), entry.getValue());
        }

        item.addEnchantments(enchants);

        return item;
    }

    public void setEnchants(Map<String, Integer> enchants) {
        this.enchants = enchants;
    }

    public void setEnchantChances(Map<String, Double> enchants) {
        this.enchantChances = enchants;
    }

    public Map<String, Integer> getEnchants() {
        return this.enchants == null ? null : Collections.unmodifiableMap(this.enchants);
    }

    public XMaterial getBurnedMaterial() {
        return this.burnedMaterial;
    }

    public void setBurnedMaterial(XMaterial burnedMaterial) {
        this.burnedMaterial = burnedMaterial;
    }

    public double getChance() {
        return this.chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public boolean runChance(int looting, ItemStack murderWeapon) {
        double chance = this.chance;

        if (this.enchantChances != null && murderWeapon != null && this.enchants != null) {
            for (Map.Entry<Enchantment, Integer> entry : murderWeapon.getEnchantments().entrySet()) {
                String key = entry.getKey().getName() + ":" + entry.getValue();

                if (!this.enchants.containsKey(key)) {
                    continue;
                }

                if (this.enchantChances.get(key) > chance) {
                    chance = this.enchantChances.get(key);
                }
            }
        }

        return (Math.random() * 100) - (chance + (this.lootingIncrease == null ? 1 : this.lootingIncrease * looting)) < 0 || chance == 100;
    }

    public int getMin() {
        return this.min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getDamageMax() {
        return this.damageMax == null ? 0 : this.damageMax;
    }

    public void setDamageMax(int damageMax) {
        this.damageMax = damageMax;
    }

    public int getDamageMin() {
        return this.damageMin == null ? 0 : this.damageMin;
    }

    public void setDamageMin(int damageMin) {
        this.damageMin = damageMin;
    }

    public int getAmountToDrop(int looting) {
        return this.min == this.max ? (this.max + getLooting(looting)) : new Random().nextInt((this.max + getLooting(looting)) - this.min + 1) + this.min;
    }

    public int getLooting(int looting) {
        return this.allowLootingEnchant ? looting : 0;
    }

    public boolean isAllowLootingEnchant() {
        return this.allowLootingEnchant;
    }

    public void setAllowLootingEnchant(boolean allowLootingEnchant) {
        this.allowLootingEnchant = allowLootingEnchant;
    }

    public void setLootingIncrease(double increase) {
        this.lootingIncrease = increase;
    }

    public void addChildLoots(Loot... loots) {
        this.childDropCountMin = 1;
        this.childDropCountMax = 1;

        if (this.childLoot == null) {
            this.childLoot = new ArrayList<>();
        }

        this.childLoot.addAll(Arrays.asList(loots));
    }

    public void removeChildLoot(Loot loot) {
        if (this.childLoot == null) {
            return;
        }

        this.childLoot.remove(loot);
    }

    public List<Loot> getChildLoot() {
        return this.childLoot == null ? new ArrayList<>() : new ArrayList<>(this.childLoot);
    }

    public List<EntityType> getOnlyDropFor() {
        return this.onlyDropFor == null ? new ArrayList<>() : new ArrayList<>(this.onlyDropFor);
    }

    public void addOnlyDropFor(EntityType... types) {
        this.onlyDropFor = new ArrayList<>();
        this.onlyDropFor.addAll(Arrays.asList(types));
    }

    public void setOnlyDropFor(List<EntityType> types) {
        this.onlyDropFor = types;
    }

    public void setChildDropCountMin(int childDropCountMin) {
        this.childDropCountMin = childDropCountMin;
    }

    public void setChildDropCountMax(int childDropCountMax) {
        this.childDropCountMax = childDropCountMax;
    }

    public Integer getChildDropCountMin() {
        return this.childDropCountMin;
    }

    public Integer getChildDropCountMax() {
        return this.childDropCountMax;
    }

    public int getChildDropCount() {
        if (this.childDropCountMin == null || this.childDropCountMax == null) {
            return 0;
        }

        return new Random().nextInt(this.childDropCountMax - this.childDropCountMin + 1) + this.childDropCountMin;
    }

    public boolean isRequireCharged() {
        return this.requireCharged;
    }

    public void setRequireCharged(boolean requireCharged) {
        this.requireCharged = requireCharged;
    }
}
