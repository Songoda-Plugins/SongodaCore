package com.songoda.core.lootables.loot;

import org.bukkit.inventory.ItemStack;

public class Drop {
    private ItemStack itemStack;

    private String command;

    private int xp;

    public Drop(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public Drop(String command) {
        this.command = command;
    }

    public Drop(int xp) {
        this.xp = xp;
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

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
