package com.songoda.core.gui.events;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public class GuiDragEvent extends GuiEvent {
    public final ItemStack cursor;
    public final ItemStack oldCursor;
    public final DragType type;
    public final Map<Integer, ItemStack> newItems;
    public final Set<Integer> rawSlots;
    public final Set<Integer> inventorySlots;
    public final InventoryDragEvent event;

    public GuiDragEvent(GuiManager manager, Gui gui, Player player, InventoryDragEvent event) {
        super(manager, gui, player);

        this.cursor = event.getCursor();
        this.oldCursor = event.getOldCursor();
        this.type = event.getType();
        this.newItems = event.getNewItems();
        this.rawSlots = event.getRawSlots();
        this.inventorySlots = event.getInventorySlots();
        this.event = event;
    }
}
