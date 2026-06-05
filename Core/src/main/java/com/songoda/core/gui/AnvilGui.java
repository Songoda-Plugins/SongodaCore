package com.songoda.core.gui;

import com.songoda.core.gui.methods.Clickable;
import com.songoda.core.nms.Nms;
import com.songoda.core.nms.anvil.AnvilCore;
import com.songoda.core.nms.anvil.CustomAnvil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Anvil GUI for text prompts
 */
public class AnvilGui extends Gui {
    final Player player;
    CustomAnvil anvil;
    List<String> endPrompt = null;

    public AnvilGui(Player player) {
        this.player = player;
    }

    public AnvilGui(Player player, Gui parent) {
        super(parent);

        this.player = player;
    }

    @NotNull
    public AnvilGui setAction(@Nullable Clickable action) {
        return (AnvilGui) setAction(2, action);
    }

    @NotNull
    public AnvilGui setAction(@Nullable ClickType type, @Nullable Clickable action) {
        return (AnvilGui) setAction(2, type, action);
    }

    protected void open() {
        this.anvil.open();
    }

    public AnvilGui setInput(ItemStack item) {
        return (AnvilGui) this.setItem(0, item);
    }

    public ItemStack getInput() {
        return this.getItem(0);
    }

    public AnvilGui setOutput(ItemStack item) {
        return (AnvilGui) this.setItem(2, item);
    }

    public AnvilGui setOutputPrompt(String str) {
        this.endPrompt = Arrays.asList(str);
        return this;
    }

    public AnvilGui setOutputPrompt(String... str) {
        this.endPrompt = Arrays.asList(str);
        return this;
    }

    public AnvilGui setOutputPrompt(List<String> str) {
        this.endPrompt = str;
        return this;
    }

    void updateOutputPrompt() {
        if (this.endPrompt != null) {
            ItemStack in = this.cellItems.get(0);

            if (in != null) {
                setItem(2, GuiUtils.createButtonItem(in, this.endPrompt));
            }
        }
    }

    public ItemStack getOutput() {
        return this.getItem(2);
    }

    public String getInputText() {
        return this.anvil != null ? this.anvil.getRenameText() : null;
    }

    @NotNull
    @Override
    protected Inventory generateInventory(@NotNull GuiManager manager) {
        this.guiManager = manager;

        createInventory();
        ItemStack item;
        if (this.cellItems.containsKey(0)) {
            item = this.cellItems.get(0);

            this.inventory.setItem(0, item);
        } else if (this.cellItems.containsKey(1)) {
            item = this.cellItems.get(1);

            this.inventory.setItem(1, item);
        } else if (!this.acceptsItems) {
            item = GuiUtils.createButtonItem(XMaterial.PAPER, " ", " ");

            this.cellItems.put(0, item);
            this.inventory.setItem(0, item);
        }

        if (this.cellItems.containsKey(2)) {
            item = this.cellItems.get(2);

            this.inventory.setItem(2, item);
        }

        return this.inventory;
    }

    @Override
    protected void createInventory() {
        AnvilCore nms = Nms.getImplementations().getAnvil();

        this.anvil = nms.createAnvil(this.player, new GuiHolder(this.guiManager, this));
        this.anvil.setCustomTitle(this.title);
        this.anvil.setLevelCost(0);

        this.inventory = this.anvil.getInventory();

        this.anvil.setOnChange(this::updateOutputPrompt);
    }
}
