package com.songoda.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class GuiListener implements Listener {

    /**
     * Returns the top inventory size safely across multiple versions.
     */
    public static int getTopInventorySize(InventoryClickEvent event) {
        try {
            return event.getView().getTopInventory().getSize();
        } catch (Throwable e) {
            // likely a version where getView() or getTopInventory() is not available
            // fallback to reflection below
        }

        try {
            // Use declared methods and set accessible to handle non-public methods across versions
            Method getView = event.getClass().getMethod("getView");
            getView.setAccessible(true);
            Object view = getView.invoke(event);

            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            Object topInventory = getTopInventory.invoke(view);

            Method getSize = topInventory.getClass().getMethod("getSize");
            getSize.setAccessible(true);
            Object sizeObj = getSize.invoke(topInventory);

            if (sizeObj instanceof Number) {
                return ((Number) sizeObj).intValue();
            }
            return sizeObj != null ? Integer.parseInt(sizeObj.toString()) : 0;
        } catch (Exception e) {
            e.printStackTrace();
            // fallback: return the clicked inventory size
            Inventory inv = event.getInventory();
            return inv != null ? inv.getSize() : 0;
        }
    }

    /**
     * Returns the top inventory size safely across multiple versions for drag events.
     */
    public static int getTopInventorySize(InventoryDragEvent event) {
        try {
            return event.getView().getTopInventory().getSize();
        } catch (Throwable e) {
            // likely a version where getView() or getTopInventory() is not available
            // fallback to reflection below
        }
        try {
            // Use declared methods and set accessible to handle non-public methods across versions
            Method getView = event.getClass().getMethod("getView");
            getView.setAccessible(true);
            Object view = getView.invoke(event);
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            Object topInventory = getTopInventory.invoke(view);
            Method getSize = topInventory.getClass().getMethod("getSize");
            getSize.setAccessible(true);
            Object sizeObj = getSize.invoke(topInventory);
            if (sizeObj instanceof Number) {
                return ((Number) sizeObj).intValue();
            }
            return sizeObj != null ? Integer.parseInt(sizeObj.toString()) : 0;
        } catch (Exception e) {
            e.printStackTrace();
            // fallback: return the clicked inventory size
            Inventory inv = event.getInventory();
            return inv != null ? inv.getSize() : 0;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GuiHolder gui) {

            if (gui.cancelClick()) {
                event.setCancelled(true);
            }

            Consumer<InventoryClickEvent> onGlobalClick = gui.getOnGlobalClick();
            Consumer<InventoryClickEvent> onTopClick = gui.getOnTopClick();
            Consumer<InventoryClickEvent> onBottomClick = gui.getOnBottomClick();

            if (onGlobalClick != null) {
                onGlobalClick.accept(event);
            }

            if (event.getRawSlot() < getTopInventorySize(event)) {
                if (onTopClick != null) {
                    onTopClick.accept(event);
                }
            } else {
                if (onBottomClick != null) {
                    onBottomClick.accept(event);
                }
            }

            if (gui instanceof PaginatedGui paginatedGui) {
                paginatedGui.handleClick(event);
                return;
            }

            if (event.getRawSlot() >= getTopInventorySize(event)) {
                return;
            }

            GuiItem item = gui.getItem(event.getSlot());

            if (item != null) {
                item.onClick(event, gui, item);
                if (item.shouldUpdate()) {
                    gui.updateItem(item);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof GuiHolder gui) {
            int topSize = getTopInventorySize(event);
            boolean affectsTop = false;
            boolean affectsBottom = false;
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize) {
                    affectsTop = true;
                } else {
                    affectsBottom = true;
                }
            }
            if (affectsTop && gui.cancelClick()) {
                event.setCancelled(true);
            }
            Consumer<InventoryDragEvent> onGlobalDrag = gui.getOnGlobalDrag();
            Consumer<InventoryDragEvent> onTopDrag = gui.getOnTopDrag();
            Consumer<InventoryDragEvent> onBottomDrag = gui.getOnBottomDrag();
            if (onGlobalDrag != null) {
                onGlobalDrag.accept(event);
            }
            if (affectsTop && onTopDrag != null) {
                onTopDrag.accept(event);
            }
            if (affectsBottom && onBottomDrag != null) {
                onBottomDrag.accept(event);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GuiHolder gui) {

            Consumer<InventoryCloseEvent> onClose = gui.getOnClose();

            if (onClose != null) {
                onClose.accept(event);
            }

            gui.onClose((Player) event.getPlayer());
        }
    }
}
