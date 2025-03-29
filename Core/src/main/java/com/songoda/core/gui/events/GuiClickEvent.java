package com.songoda.core.gui.events;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiClickEvent extends GuiEvent {
    public final int slot;
    public final boolean guiClicked;
    public final ItemStack cursor, clickedItem;
    public final ClickType clickType;
    public final InventoryClickEvent event;

    public GuiClickEvent(GuiManager manager, Gui gui, Player player, InventoryClickEvent event, int slot, boolean guiClicked) {
        super(manager, gui, player);

        this.slot = slot;
        this.guiClicked = guiClicked;
        this.cursor = event.getCursor();

        Inventory clicked = event.getClickedInventory();

        this.clickedItem = clicked == null ? null : clicked.getItem(event.getSlot());
        this.clickType = event.getClick();
        this.event = event;
    }
}
