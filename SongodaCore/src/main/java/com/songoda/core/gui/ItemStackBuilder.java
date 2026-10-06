package com.songoda.core.gui;

import com.songoda.core.SongodaCore;
import com.songoda.core.item.resolver.ItemResolverManager;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.text.MiniMessagePlaceholder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ItemStackBuilder {

    private ItemStack itemStack;

    public ItemStackBuilder() {
        this.itemStack = new ItemStack(Material.AIR);
    }

    public ItemStackBuilder(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack : new ItemStack(Material.AIR);
    }

    public ItemStackBuilder(Material material) {
        this.itemStack = material != null ? new ItemStack(material) : new ItemStack(Material.AIR);
    }

    public ItemStackBuilder(String material) {
        this();
        setMaterial(material);
    }

    public ItemStackBuilder setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    public ItemStackBuilder setType(Material material) {
        itemStack.setType(material);
        return this;
    }

    public ItemStackBuilder setMaterial(Material material) {
        return setType(material);
    }

    public ItemStackBuilder setMaterial(String material) {
        if (material != null && !material.isEmpty()) {
            ItemStack resolved = ItemResolverManager.resolve(material);
            if (resolved != null) {
                int currentAmount = this.itemStack != null ? this.itemStack.getAmount() : 1;
                if (currentAmount > 1) {
                    resolved.setAmount(currentAmount);
                }
                this.itemStack = resolved;
            }
        }
        return this;
    }

    public ItemStackBuilder setAmount(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set amount for AIR ItemStack");
        }
        itemStack.setAmount(amount);
        return this;
    }

    public ItemStackBuilder setDisplayName(String displayName) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set display name for AIR ItemStack");
        }
        AdventureUtils.formatItemName(itemStack, displayName);
        return this;
    }

    public ItemStackBuilder setDisplayName(String displayName, MiniMessagePlaceholder... placeholders) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set display name for AIR ItemStack");
        }
        AdventureUtils.formatItemName(itemStack, displayName, placeholders);
        return this;
    }

    public ItemStackBuilder setLore(LoreBuilder loreBuilder) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, loreBuilder.lore);
        return this;
    }

    public ItemStackBuilder setLore(String... lore) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, lore);
        return this;
    }

    public ItemStackBuilder setLore(List<String> lore, MiniMessagePlaceholder... placeholders) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set lore for AIR ItemStack");
        }
        AdventureUtils.formatItemLore(itemStack, lore, placeholders);
        return this;
    }

    public ItemStackBuilder setModelData(int modelData) {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot set model data for AIR ItemStack");
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(modelData);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemStackBuilder addGlow() {
        if (itemStack.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot add glow to AIR ItemStack");
        }
        //AdventureUtils.addGlow(itemStack);
        return this;
    }

    public ItemStackBuilder modify(Consumer<ItemStack> consumer) {
        consumer.accept(itemStack);
        return this;
    }

    public ItemStack build() {
        if (itemStack.getType() == Material.AIR) {
            SongodaCore.getPlugin().getLogger().warning("ItemStack is AIR in ItemStackBuilder. Please set a material before building.");
            new Throwable().printStackTrace();
        }
        return itemStack;
    }

    public static class LoreBuilder {

        private List<String> lore = new LinkedList<>();

        public LoreBuilder addLine(String line) {
            lore.add(line);
            return this;
        }

        public LoreBuilder addLineIf(boolean condition, String line) {
            if (condition) {
                lore.add(line);
            }
            return this;
        }
    }
}
