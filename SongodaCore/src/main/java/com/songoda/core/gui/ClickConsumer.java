package com.songoda.core.gui;

import org.bukkit.event.inventory.InventoryEvent;

public interface ClickConsumer<E extends InventoryEvent, G extends GuiHolder, I extends GuiItem> {

    void accept(E event, G gui, I guiItem);
}
