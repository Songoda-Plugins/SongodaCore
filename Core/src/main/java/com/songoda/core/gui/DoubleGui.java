package com.songoda.core.gui;

import com.songoda.core.gui.events.GuiClickEvent;
import com.songoda.core.gui.events.GuiDropItemEvent;
import com.songoda.core.gui.methods.Clickable;
import com.songoda.core.gui.methods.Closable;
import com.songoda.core.gui.methods.Droppable;
import com.songoda.core.gui.methods.Openable;
import com.songoda.core.gui.methods.Pagable;
import com.songoda.core.utils.ItemUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: does not restore inventory if server crashes while player inventory is open
 * Method to fix: save inv + ender slot to file, store paper in ender inv with name of cache file, check for paper item in slot when loading
 * Or just manually manage all inventories in a file and remove when restored
 */
public class DoubleGui extends Gui {
    protected boolean startStashed = true;
    protected int playerRows = 4;
    protected Map<Player, ItemStack[]> stash = new HashMap<>();

    public DoubleGui() {
        super(GuiType.STANDARD);
        this.allowDropItems = false;
    }

    public DoubleGui(GuiType type) {
        super(type);
        this.allowDropItems = false;
    }

    public DoubleGui(int rows) {
        super(rows);
        this.allowDropItems = false;
    }

    public DoubleGui(int rows, Gui parent) {
        super(rows, parent);
        this.allowDropItems = false;
    }

    public DoubleGui(Gui parent) {
        super(parent);
        this.allowDropItems = false;
    }

    public int getPlayerRows() {
        return this.playerRows;
    }

    // 9  -> 0  -> 54
    // 18 -> 9  -> 63
    // 27 -> 18 -> 72
    // 0  -> 27 -> 81
    // offset required to make click translations
    int clickOffset(int cell) {
        return 54 + (cell < 9 ? cell + 27 : cell - 9);
    }

    // offset required to make inventory translations
    int invOffset(int cell) {
        return 54 + cell;
    }

    public void setStartStashed(boolean startStashed) {
        this.startStashed = startStashed;
    }

    public DoubleGui setPlayerUnlocked(int cell) {
        this.unlockedCells.put(invOffset(cell), true);
        return this;
    }

    public DoubleGui setPlayerUnlocked(int row, int col) {
        this.unlockedCells.put(invOffset(col + row * 9), true);
        return this;
    }

    public DoubleGui setPlayerUnlocked(int cell, boolean open) {
        this.unlockedCells.put(invOffset(cell), open);
        return this;
    }

    public DoubleGui setPlayerUnlocked(int row, int col, boolean open) {
        this.unlockedCells.put(invOffset(col + row * 9), open);
        return this;
    }

    public DoubleGui setPlayerUnlockedRange(int cellFirst, int cellLast) {
        final int last = invOffset(cellLast);
        for (int cell = invOffset(cellFirst); cell <= last; ++cell) {
            this.unlockedCells.put(cell, true);
        }
        return this;
    }

    public DoubleGui setPlayerUnlockedRange(int cellFirst, int cellLast, boolean open) {
        final int last = invOffset(cellLast);

        for (int cell = invOffset(cellFirst); cell <= last; ++cell) {
            this.unlockedCells.put(cell, open);
        }

        return this;
    }

    public DoubleGui setPlayerUnlockedRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast) {
        final int last = invOffset(cellColLast + cellRowLast * 9);

        for (int cell = invOffset(cellColFirst + cellRowFirst * 9); cell <= last; ++cell) {
            this.unlockedCells.put(cell, true);
        }

        return this;
    }

    public DoubleGui setPlayerUnlockedRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, boolean open) {
        final int last = invOffset(cellColLast + cellRowLast * 9);

        for (int cell = invOffset(cellColFirst + cellRowFirst * 9); cell <= last; ++cell) {
            this.unlockedCells.put(cell, open);
        }

        return this;
    }

    public DoubleGui setPlayerItem(int cell, ItemStack item) {
        this.cellItems.put(invOffset(cell), item);

        if (this.open && cell >= 0 && cell < 36) {
            cell = cell >= 27 ? cell - 27 : cell + 9;

            for (HumanEntity e : this.inventory.getViewers()) {
                e.getInventory().setItem(cell, item);
            }
        }

        return this;
    }

    public DoubleGui setPlayerItem(int row, int col, ItemStack item) {
        int cell = col + row * 9;
        this.cellItems.put(invOffset(cell), item);

        if (this.open && cell >= 0 && cell < 36) {
            cell = cell >= 27 ? cell - 27 : cell + 9;

            for (HumanEntity e : this.inventory.getViewers()) {
                e.getInventory().setItem(cell, item);
            }
        }

        return this;
    }

    public DoubleGui highlightPlayerItem(int cell) {
        final int invCell = invOffset(cell);
        ItemStack item = this.cellItems.get(invCell);

        if (item != null) {
            setPlayerItem(cell, ItemUtils.addGlow(item));
        }

        return this;
    }

    public DoubleGui highlightPlayerItem(int row, int col) {
        final int cell = col + row * 9;
        final int invCell = invOffset(cell);

        ItemStack item = this.cellItems.get(invCell);
        if (item != null) {
            setPlayerItem(cell, ItemUtils.addGlow(item));
        }

        return this;
    }

    public DoubleGui setPlayerAction(int cell, Clickable action) {
        setConditional(invOffset(cell), null, action);
        return this;
    }

    public DoubleGui setPlayerAction(int row, int col, Clickable action) {
        setConditional(invOffset(col + row * 9), null, action);
        return this;
    }

    public DoubleGui setPlayerAction(int cell, ClickType type, Clickable action) {
        setConditional(invOffset(cell), type, action);
        return this;
    }

    public DoubleGui setPlayerAction(int row, int col, ClickType type, Clickable action) {
        setConditional(invOffset(col + row * 9), type, action);
        return this;
    }

    public DoubleGui setPlayerActionForRange(int cellFirst, int cellLast, Clickable action) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            setConditional(invOffset(cell), null, action);
        }

        return this;
    }

    public DoubleGui setPlayerActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, Clickable action) {
        final int last = cellColLast + cellRowLast * 9;

        for (int cell = cellColFirst + cellRowFirst * 9; cell <= last; ++cell) {
            setConditional(invOffset(cell), null, action);
        }

        return this;
    }

    public DoubleGui setPlayerActionForRange(int cellFirst, int cellLast, ClickType type, Clickable action) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            setConditional(invOffset(cell), type, action);
        }

        return this;
    }

    public DoubleGui setPlayerActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, ClickType type, Clickable action) {
        final int last = cellColLast + cellRowLast * 9;

        for (int cell = cellColFirst + cellRowFirst * 9; cell <= last; ++cell) {
            setConditional(invOffset(cell), type, action);
        }

        return this;
    }

    public DoubleGui clearPlayerActions(int cell) {
        this.conditionalButtons.remove(invOffset(cell));
        return this;
    }

    public DoubleGui clearPlayerActions(int row, int col) {
        final int cell = invOffset(col + row * 9);
        this.conditionalButtons.remove(cell);

        return this;
    }

    public DoubleGui setPlayerButton(int cell, ItemStack item, Clickable action) {
        setPlayerItem(cell, item);
        setConditional(invOffset(cell), null, action);

        return this;
    }

    public DoubleGui setPlayerButton(int row, int col, ItemStack item, Clickable action) {
        final int cell = col + row * 9;

        setPlayerItem(cell, item);
        setConditional(invOffset(cell), null, action);

        return this;
    }

    public DoubleGui setPlayerButton(int cell, ItemStack item, ClickType type, Clickable action) {
        setItem(cell, item);
        setConditional(invOffset(cell), type, action);

        return this;
    }

    public DoubleGui setPlayerButton(int row, int col, ItemStack item, ClickType type, Clickable action) {
        final int cell = col + row * 9;

        setPlayerItem(cell, item);
        setConditional(invOffset(cell), type, action);

        return this;
    }

    @Override
    protected boolean onClickPlayerInventory(@NotNull GuiManager manager, @NotNull Player player, @NotNull Inventory openInv, InventoryClickEvent event) {
        final int cell = event.getSlot(), offsetCell = clickOffset(cell);
        Map<ClickType, Clickable> conditionals = this.conditionalButtons.get(offsetCell);
        Clickable button;

        if (conditionals != null
                && ((button = conditionals.get(event.getClick())) != null || (button = conditionals.get(null)) != null)) {
            button.onClick(new GuiClickEvent(manager, this, player, event, cell, true));
        } else {
            // no event for this button
            return false;
        }

        event.setCancelled(this.unlockedCells.entrySet().stream().noneMatch(e -> offsetCell == e.getKey() && e.getValue()));

        return true;
    }

    @Override
    protected boolean onClickOutside(@NotNull GuiManager manager, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (this.dropper != null) {
            return this.dropper.onDrop(new GuiDropItemEvent(manager, this, player, event));
        }

        // do not allow by default
        return false;
    }

    @Override
    public void onOpen(@NotNull GuiManager manager, @NotNull Player player) {
        // replace the player's inventory
        if (this.startStashed) {
            stashItems(player);
        }

        // other opening functions
        super.onOpen(manager, player);
    }

    @Override
    public void onClose(@NotNull GuiManager manager, @NotNull Player player) {
        // restore the player's inventory
        restoreStash(player);

        // other closing functions
        super.onClose(manager, player);
    }

    protected void restoreStash(Player player) {
        if (this.stash.containsKey(player)) {
            player.getInventory().setContents(this.stash.remove(player));
            player.updateInventory();
        }
    }

    protected void stashItems(Player player) {
        if (!this.stash.containsKey(player)) {
            this.stash.put(player, player.getInventory().getContents().clone());
            player.getInventory().clear();
        }
    }

    /*
     *********************************************************
     * Other functions from GUI that we don't actually override
     *********************************************************
     */
    @Override
    public DoubleGui setAcceptsItems(boolean acceptsItems) {
        return (DoubleGui) super.setAcceptsItems(acceptsItems);
    }

    @Override
    public @NotNull DoubleGui setUnlocked(int cell) {
        return (DoubleGui) super.setUnlocked(cell);
    }

    @Override
    public @NotNull DoubleGui setUnlocked(int cell, boolean open) {
        return (DoubleGui) super.setUnlocked(cell, open);
    }

    @Override
    public @NotNull DoubleGui setUnlocked(int row, int col) {
        return (DoubleGui) super.setUnlocked(row, col);
    }

    @Override
    public @NotNull DoubleGui setUnlocked(int row, int col, boolean open) {
        return (DoubleGui) super.setUnlocked(row, col, open);
    }

    @Override
    public @NotNull DoubleGui setUnlockedRange(int cellFirst, int cellLast) {
        return (DoubleGui) super.setUnlockedRange(cellFirst, cellLast);
    }

    @Override
    public @NotNull DoubleGui setUnlockedRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast) {
        return (DoubleGui) super.setUnlockedRange(cellRowFirst, cellColFirst, cellRowLast, cellColLast);
    }

    @Override
    public DoubleGui setAllowDrops(boolean allow) {
        return (DoubleGui) super.setAllowDrops(allow);
    }

    @Override
    public DoubleGui setAllowClose(boolean allow) {
        return (DoubleGui) super.setAllowClose(allow);
    }

    @Override
    public @NotNull DoubleGui setTitle(String title) {
        return (DoubleGui) super.setTitle(title);
    }

    @Override
    public @NotNull DoubleGui setRows(int rows) {
        return (DoubleGui) super.setRows(rows);
    }

    @Override
    public @NotNull DoubleGui setDefaultItem(ItemStack item) {
        return (DoubleGui) super.setDefaultItem(item);
    }

    @Override
    public @NotNull DoubleGui setItem(int cell, ItemStack item) {
        return (DoubleGui) super.setItem(cell, item);
    }

    @Override
    public @NotNull DoubleGui setItem(int row, int col, ItemStack item) {
        return (DoubleGui) super.setItem(row, col, item);
    }

    @Override
    public @NotNull DoubleGui highlightItem(int cell) {
        return (DoubleGui) super.highlightItem(cell);
    }

    @Override
    public @NotNull DoubleGui highlightItem(int row, int col) {
        return (DoubleGui) super.highlightItem(row, col);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, String name, String... lore) {
        return (DoubleGui) super.updateItem(cell, name, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int row, int col, String name, List<String> lore) {
        return (DoubleGui) super.updateItem(col + row * 9, name, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, @NotNull String name, List<String> lore) {
        return (DoubleGui) super.updateItem(cell, name, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int row, int col, @NotNull ItemStack itemTo, String title, String... lore) {
        return (DoubleGui) super.updateItem(col + row * 9, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, @NotNull ItemStack itemTo, String title, String... lore) {
        return (DoubleGui) super.updateItem(cell, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int row, int col, @NotNull XMaterial itemTo, String title, String... lore) {
        return (DoubleGui) super.updateItem(col + row * 9, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, @NotNull XMaterial itemTo, String title, String... lore) {
        return (DoubleGui) super.updateItem(cell, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int row, int col, @NotNull ItemStack itemTo, String title, List<String> lore) {
        return (DoubleGui) super.updateItem(col + row * 9, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, @NotNull ItemStack itemTo, String title, List<String> lore) {
        return (DoubleGui) super.updateItem(cell, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int row, int col, @NotNull XMaterial itemTo, String title, List<String> lore) {
        return (DoubleGui) super.updateItem(col + row * 9, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui updateItem(int cell, @NotNull XMaterial itemTo, String title, List<String> lore) {
        return (DoubleGui) super.updateItem(cell, itemTo, title, lore);
    }

    @Override
    public @NotNull DoubleGui setAction(int cell, Clickable action) {
        return (DoubleGui) super.setAction(cell, action);
    }

    @Override
    public @NotNull DoubleGui setAction(int row, int col, Clickable action) {
        return (DoubleGui) super.setAction(row, col, action);
    }

    @Override
    public @NotNull DoubleGui setAction(int cell, ClickType type, Clickable action) {
        return (DoubleGui) super.setAction(cell, type, action);
    }

    @Override
    public @NotNull DoubleGui setAction(int row, int col, ClickType type, Clickable action) {
        return (DoubleGui) super.setAction(row, col, type, action);
    }

    @Override
    public @NotNull DoubleGui setActionForRange(int cellFirst, int cellLast, Clickable action) {
        return (DoubleGui) super.setActionForRange(cellFirst, cellLast, action);
    }

    @Override
    public @NotNull DoubleGui setActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, Clickable action) {
        return (DoubleGui) super.setActionForRange(cellRowFirst, cellColFirst, cellRowLast, cellColLast, action);
    }

    @Override
    public @NotNull DoubleGui setActionForRange(int cellFirst, int cellLast, ClickType type, Clickable action) {
        return (DoubleGui) super.setActionForRange(cellFirst, cellLast, type, action);
    }

    @Override
    public @NotNull DoubleGui setActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, ClickType type, Clickable action) {
        return (DoubleGui) super.setActionForRange(cellRowFirst, cellColFirst, cellRowLast, cellColLast, type, action);
    }

    @Override
    public @NotNull DoubleGui clearActions(int cell) {
        return (DoubleGui) super.clearActions(cell);
    }

    @Override
    public @NotNull DoubleGui clearActions(int row, int col) {
        return (DoubleGui) super.clearActions(row, col);
    }

    @Override
    public @NotNull DoubleGui setButton(int cell, ItemStack item, Clickable action) {
        return (DoubleGui) super.setButton(cell, item, action);
    }

    @Override
    public @NotNull DoubleGui setButton(int row, int col, ItemStack item, Clickable action) {
        return (DoubleGui) super.setButton(row, col, item, action);
    }

    @Override
    public @NotNull DoubleGui setButton(int cell, ItemStack item, ClickType type, Clickable action) {
        return (DoubleGui) super.setButton(cell, item, type, action);
    }

    @Override
    public @NotNull DoubleGui setButton(int row, int col, ItemStack item, ClickType type, Clickable action) {
        return (DoubleGui) super.setButton(row, col, item, type, action);
    }

    @Override
    public @NotNull DoubleGui setOnOpen(Openable action) {
        return (DoubleGui) super.setOnOpen(action);
    }

    @Override
    public @NotNull DoubleGui setOnClose(Closable action) {
        return (DoubleGui) super.setOnClose(action);
    }

    @Override
    public @NotNull DoubleGui setOnDrop(Droppable action) {
        return (DoubleGui) super.setOnDrop(action);
    }

    @Override
    public @NotNull DoubleGui setOnPage(Pagable action) {
        return (DoubleGui) super.setOnPage(action);
    }

    @Override
    public @NotNull DoubleGui setNextPage(int row, int col, @NotNull ItemStack item) {
        return (DoubleGui) super.setNextPage(row, col, item);
    }

    @Override
    public @NotNull DoubleGui setPrevPage(int row, int col, @NotNull ItemStack item) {
        return (DoubleGui) super.setPrevPage(row, col, item);
    }
}
