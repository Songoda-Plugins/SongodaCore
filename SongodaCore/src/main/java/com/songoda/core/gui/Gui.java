package com.songoda.core.gui;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vinject.config.ConfigurationSection;
import com.songoda.core.SongodaCore;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.item.resolver.ItemResolverManager;
import com.songoda.core.text.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Inventory GUI. Titles and item names/lore go through {@link com.songoda.core.text.AdventureUtils},
 * which uses Paper's native Adventure API.
 */
public class Gui implements GuiHolder {

    public static String BACK_BUTTON_NAME = "§cBack";
    @Getter
    private final UUID uuid = java.util.UUID.randomUUID();
    @Getter
    private final int rows;
    private final Inventory inventory;
    private final List<GuiItem> items = new ArrayList<>();
    private final List<Player> openers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoUpdatePending = new AtomicBoolean();
    @Getter
    private Consumer<InventoryClickEvent> onGlobalClick;
    @Getter
    private Consumer<InventoryClickEvent> onTopClick;
    @Getter
    private Consumer<InventoryClickEvent> onBottomClick;
    @Getter
    private Consumer<InventoryDragEvent> onGlobalDrag;
    @Getter
    private Consumer<InventoryDragEvent> onTopDrag;
    @Getter
    private Consumer<InventoryDragEvent> onBottomDrag;
    @Getter
    private Consumer<InventoryCloseEvent> onClose;
    private boolean cancelClick = true;
    private Gui previusGui = null;

    public Gui(String name, int rows) {
        this.rows = rows;
        this.inventory = AdventureUtils.createInventory(this, rows, name);
    }

    public Gui(Component name, int rows) {
        this.rows = rows;
        this.inventory = AdventureUtils.createInventory(this, rows, name);
    }

    public Gui setPreviusGui(Gui gui) {
        this.previusGui = gui;
        //Add a back button to the bottom left corner
        ItemStack back = new ItemStack(Material.ARROW);
        AdventureUtils.formatItemName(back, BACK_BUTTON_NAME);
        addItem(new GuiItem(back, (event, gui1, guiItem) -> {
            if (previusGui != null) {
                //Check if any of the items needs to be re-drawn
                previusGui.reDrawIfNeeded();
                previusGui.show((Player) event.getWhoClicked());
            }
        }, 0, rows - 1));
        return this;
    }

    private void reDrawIfNeeded() {
        boolean needsRedraw = false;
        Iterator<GuiItem> it = items.iterator();
        while (it.hasNext()) {
            GuiItem item = it.next();
            if (item.shouldUpdate()) {
                needsRedraw = true;
                item.updateItem();
                if (item.getItem().getType() == Material.AIR) {
                    items.remove(item);
                    continue;
                }
            }
        }
        if (needsRedraw) {
            refresh();
        }
    }

    public Gui addItem(GuiItem item) {
        //Check if x or y is -1, if so we ignore the item and throw an error
        if (item.getX() == -1 || item.getY() == -1) {
            SongodaCore.getPlugin().getLogger().warning("Tried to add an item with x or y set to -1, ignoring it. These values are reserved for PaginatedGui");
            return this;
        }

        //Check if we have an item in the same slot, if so override it
        items.removeIf(i -> i.getX() == item.getX() && i.getY() == item.getY());

        items.add(item);
        inventory.setItem(item.getX() + item.getY() * 9, item.getItem());
        return this;
    }

    /**
     * Adds an item to the gui
     *
     * @param item  the item to add
     * @param event the event to run when the item is clicked
     * @param x     the x coordinate starting from 0
     * @param y     the y coordinate starting from 0
     * @return the gui
     */
    public Gui addItem(ItemStack item, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event, int x, int y) {
        return addItem(new GuiItem(item, event, x, y));
    }

    /**
     * Adds a static item to the gui
     *
     * @param item the item to add
     * @param x    the x coordinate starting from 0
     * @param y    the y coordinate starting from 0
     * @return the gui
     */
    public Gui addItem(ItemStack item, int x, int y) {
        return addItem(new GuiItem(item, x, y));
    }

    /**
     * Adds a dynamic item to the gui
     *
     * @param itemStackBuilder the item builder, type must be changed to non-air
     * @param event            the event to run when the item is clicked
     * @param x                the x coordinate starting from 0
     * @param y                the y coordinate starting from 0
     * @return the gui
     */
    public Gui addItem(Consumer<ItemStackBuilder> itemStackBuilder, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event, int x, int y) {
        return addItem(new GuiItem(itemStackBuilder, event, x, y));
    }

    public void removeItem(GuiItem item) {
        items.remove(item);
    }

    /**
     * Only refreshes the items that need to be updated
     */
    public void refresh() {
        refresh(false);
    }

    /**
     * Refreshes all items
     *
     * @param clear if true, clears the inventory before refreshing
     */
    public void refresh(boolean clear) {
        if (clear) {
            inventory.clear();
            for (GuiItem item : items) {
                item.updateItem();
                inventory.setItem(item.getX() + item.getY() * 9, item.getItem());
            }
        } else {
            for (GuiItem item : items) {
                if (item.shouldUpdate()) {
                    item.updateItem();
                    inventory.setItem(item.getX() + item.getY() * 9, item.getItem());
                }
            }
        }
    }

    public void reopen(Player player) {
        show(player);
    }

    public void reopenAll() {
        for (Player player : openers) {
            show(player);
        }
    }

    public Gui show(Player player) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(player)) {
            SchedulerUtils.runEntityTask(SongodaCore.getPlugin(), player, () -> show(player));
            return this;
        }
        player.openInventory(inventory);
        if (!openers.contains(player)) {
            openers.add(player);
        }
        GuiManager.markAsOpen(this);
        return this;
    }

    public void open(Player player) {
        show(player);
    }

    public Gui close(Player player) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(player)) {
            if (SongodaCore.getPlugin().isEnabled()) {
                SchedulerUtils.runEntityTask(SongodaCore.getPlugin(), player, () -> close(player));
            }
            return this;
        }

        player.closeInventory();
        openers.remove(player);
        if (openers.isEmpty()) {
            GuiManager.markAsClosed(this);
        }
        return this;
    }

    public Gui closeAll() {
        // Copy: closeInventory() fires InventoryCloseEvent synchronously and onClose() mutates openers.
        for (Player player : openers) {
            close(player);
        }
        if (openers.isEmpty()) {
            GuiManager.markAsClosed(this);
        }
        return this;
    }

    @Override
    public void autoUpdate() {
        GuiManager.autoUpdate(openers, autoUpdatePending, () -> {
            List<GuiItem> itemsToUpdate = getItems().stream().filter(guiItem -> guiItem.getState() == State.AUTO_UPDATE).toList();
            for (GuiItem item : itemsToUpdate) {
                updateItem(item);
            }
        });
    }

    public List<Player> getOpeners() {
        return Collections.unmodifiableList(openers);
    }

    public Gui setCancelClick(boolean cancelClick) {
        this.cancelClick = cancelClick;
        return this;
    }

    public boolean cancelClick() {
        return cancelClick;
    }

    public @Nullable GuiItem getItem(int slot) {
        int x = slot % 9;
        int y = slot / 9;

        for (GuiItem item : items) {
            if (item.getX() == x && item.getY() == y) {
                return item;
            }
        }
        return null;
    }

    @Override
    public @NotNull List<GuiItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Gui setOnGlobalClick(Consumer<InventoryClickEvent> onGlobalClick) {
        this.onGlobalClick = onGlobalClick;
        return this;
    }

    public Gui setOnTopClick(Consumer<InventoryClickEvent> onTopClick) {
        this.onTopClick = onTopClick;
        return this;
    }

    public Gui setOnBottomClick(Consumer<InventoryClickEvent> onBottomClick) {
        this.onBottomClick = onBottomClick;
        return this;
    }

    public Gui setOnGlobalDrag(Consumer<InventoryDragEvent> onGlobalDrag) {
        this.onGlobalDrag = onGlobalDrag;
        return this;
    }

    public Gui setOnTopDrag(Consumer<InventoryDragEvent> onTopDrag) {
        this.onTopDrag = onTopDrag;
        return this;
    }

    public Gui setOnBottomDrag(Consumer<InventoryDragEvent> onBottomDrag) {
        this.onBottomDrag = onBottomDrag;
        return this;
    }

    public Gui setOnClose(Consumer<InventoryCloseEvent> onClose) {
        this.onClose = onClose;
        return this;
    }

    public Gui fetchFills(ConfigurationSection config) {
        String fillEmpty = config.getString("Fill Empty");
        if (fillEmpty != null) {
            ItemStack fillEmptyItem = ItemResolverManager.resolve(fillEmpty);
            if (fillEmptyItem != null) {
                fillEmpty(fillEmptyItem);
            }
        }

        String fillBorder = config.getString("Fill Border");
        if (fillBorder != null) {
            ItemStack fillBorderItem = ItemResolverManager.resolve(fillBorder);
            if (fillBorderItem != null) {
                fillBorder(fillBorderItem);
            }
        }

        String fillBottom = config.getString("Fill Bottom");
        if (fillBottom != null) {
            ItemStack fillBottomItem = ItemResolverManager.resolve(fillBottom);
            if (fillBottomItem != null) {
                fillBottom(fillBottomItem);
            }
        }
        return this;
    }

    public Gui fillEmpty(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                addItem(new GuiItem(item, i % 9, i / 9));
            }
        }
        return this;
    }

    public Gui fillEmpty(Material material) {
        return fillEmpty(new ItemStack(material));
    }

    public Gui fillBorder(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");

        //Fill first row
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) != null) continue;
            inventory.setItem(i, item);
            addItem(new GuiItem(item, i, 0));
        }

        //Fill first column and last column
        for (int i = 1; i < inventory.getSize() / 9 - 1; i++) {
            if (inventory.getItem(i * 9) != null) continue;
            addItem(new GuiItem(item, 0, i));
            addItem(new GuiItem(item, 8, i));
        }

        //Fill last row
        for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) != null) continue;
            addItem(new GuiItem(item, i % 9, i / 9));
        }
        return this;
    }

    public Gui fillBorder(Material material) {
        return fillBorder(new ItemStack(material));
    }

    public Gui fillBottom(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");

        for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) != null) continue;
            addItem(new GuiItem(item, i % 9, i / 9));
        }
        return this;
    }

    public Gui fillBottom(Material material) {
        return fillBottom(new ItemStack(material));
    }

    @Override
    public void onClose(Player player) {
        //Player closed the inventory, lets remove him from the openers list if needed
        openers.remove(player);
        if (openers.isEmpty()) {
            GuiManager.markAsClosed(this);
        }
    }

    @Override
    public void updateItem(GuiItem item) {
        inventory.setItem(item.getX() + item.getY() * 9, item.updateItem());
    }

    @Override
    public void updateItemAt(int slot) {
        GuiItem item = getItem(slot);
        if (item != null) {
            inventory.setItem(slot, item.updateItem());
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Gui gui = (Gui) obj;
        return this.uuid.equals(gui.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}
