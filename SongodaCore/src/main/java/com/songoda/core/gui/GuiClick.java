package com.songoda.core.gui;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;

@Getter
public class GuiClick {

    private final InventoryClickEvent event;
    private final Gui gui;
    private final GuiItem guiItem;

    public GuiClick(InventoryClickEvent event, Gui gui, GuiItem guiItem) {
        this.event = event;
        this.gui = gui;
        this.guiItem = guiItem;
    }
}
