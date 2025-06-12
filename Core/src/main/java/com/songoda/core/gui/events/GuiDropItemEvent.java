package com.songoda.core.gui.events;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiDropItemEvent extends GuiEvent {
    public final ItemStack cursor;
    public final ClickType clickType;
    public final InventoryClickEvent event;

    public GuiDropItemEvent(GuiManager manager, Gui gui, Player player, InventoryClickEvent event) {
        super(manager, gui, player);

        this.cursor = event.getCursor();
        this.clickType = event.getClick();
        this.event = event;
    }
}
