package com.songoda.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a GUI holder.
 */
public interface GuiHolder extends InventoryHolder {

    Consumer<InventoryDragEvent> getOnGlobalDrag();

    GuiHolder setOnGlobalDrag(Consumer<InventoryDragEvent> onGlobalDrag);

    Consumer<InventoryDragEvent> getOnTopDrag();

    GuiHolder setOnTopDrag(Consumer<InventoryDragEvent> onTopDrag);

    Consumer<InventoryDragEvent> getOnBottomDrag();

    GuiHolder setOnBottomDrag(Consumer<InventoryDragEvent> onBottomDrag);

    Consumer<InventoryClickEvent> getOnGlobalClick();

    GuiHolder setOnGlobalClick(Consumer<InventoryClickEvent> onGlobalClick);

    Consumer<InventoryClickEvent> getOnTopClick();

    GuiHolder setOnTopClick(Consumer<InventoryClickEvent> onTopClick);

    Consumer<InventoryClickEvent> getOnBottomClick();

    GuiHolder setOnBottomClick(Consumer<InventoryClickEvent> onBottomClick);

    Consumer<InventoryCloseEvent> getOnClose();

    GuiHolder setOnClose(Consumer<InventoryCloseEvent> onClose);

    boolean cancelClick();

    @Nullable
    GuiItem getItem(int slot);

    @NotNull List<GuiItem> getItems();

    void onClose(Player player);

    void updateItem(GuiItem item);

    void updateItemAt(int slot);

    GuiHolder closeAll();

    void autoUpdate();
}
