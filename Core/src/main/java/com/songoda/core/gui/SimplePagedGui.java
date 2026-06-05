package com.songoda.core.gui;

import com.songoda.core.gui.events.GuiClickEvent;
import com.songoda.core.gui.methods.Clickable;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Paged GUI for when you aren't going to be making too many pages
 */
public class SimplePagedGui extends Gui {
    protected boolean useHeader;
    private int rowsPerPage, maxCellSlot;
    protected ItemStack headerBackItem;
    protected ItemStack footerBackItem;
    final int nextPageIndex = 4, prevPageIndex = 6;

    public SimplePagedGui() {
        this(null);
    }

    public SimplePagedGui(Gui parent) {
        super(parent);

        this.nextPage = GuiUtils.createButtonItem(XMaterial.ARROW, "Next Page");
        this.prevPage = GuiUtils.createButtonItem(XMaterial.ARROW, "Previous Page");
    }

    public SimplePagedGui setUseHeader(boolean useHeader) {
        this.useHeader = useHeader;
        return this;
    }

    public ItemStack getHeaderBackItem() {
        return this.headerBackItem;
    }

    public SimplePagedGui setHeaderBackItem(ItemStack headerBackItem) {
        this.headerBackItem = headerBackItem;
        return this;
    }

    public ItemStack getFooterBackItem() {
        return this.footerBackItem;
    }

    public SimplePagedGui setFooterBackItem(ItemStack footerBackItem) {
        this.footerBackItem = footerBackItem;
        return this;
    }

    @Override
    public @NotNull SimplePagedGui setItem(int row, int col, ItemStack item) {
        return setItem(col + row * 9, item);
    }

    @Override
    public @NotNull SimplePagedGui setItem(int cell, ItemStack item) {
        // set the cell relative to the current page
        int cellIndex = cell < 0 ? cell : (this.page == 1 || (this.useHeader && cell < 9) ? cell : (cell + (this.page - 1) * (this.rowsPerPage * 9)));

        this.cellItems.put(cellIndex, item);
        if (this.open && cell >= 0 && cell < this.inventory.getSize()) {
            this.inventory.setItem(cell, item);
        }

        return this;
    }

    @Override
    public void nextPage() {
        if (this.page < this.pages) {
            ++this.page;
            showPage();
        }
    }

    @Override
    public void prevPage() {
        if (this.page > 1) {
            --this.page;
            showPage();
        }
    }

    public void showPage() {
        int startCell = this.useHeader ? 9 : 0;
        int cellIndex = startCell + (this.page - 1) * (this.rowsPerPage * 9);

        for (int i = startCell; i < (this.rows - 1) * 9; ++i) {
            final ItemStack item = this.cellItems.get(cellIndex++);
            this.inventory.setItem(i, item != null ? item : this.blankItem);
        }

        // page markers
        updatePageNavigation();
    }

    @Override
    protected void updatePageNavigation() {
        if (this.page > 1) {
            this.inventory.setItem(this.inventory.getSize() - this.prevPageIndex, this.prevPage);

            this.setButton(-this.prevPageIndex, this.prevPage, ClickType.LEFT, (event) -> this.prevPage());
        } else {
            this.inventory.setItem(this.inventory.getSize() - this.prevPageIndex, this.footerBackItem != null ? this.footerBackItem : this.blankItem);

            this.setItem(-this.prevPageIndex, null);
            this.clearActions(-this.prevPageIndex);
        }

        if (this.pages > 1 && this.page != this.pages) {
            this.inventory.setItem(this.inventory.getSize() - this.nextPageIndex, this.nextPage);

            this.setButton(-this.nextPageIndex, this.nextPage, ClickType.LEFT, (event) -> this.nextPage());
        } else {
            this.inventory.setItem(this.inventory.getSize() - this.nextPageIndex, this.footerBackItem != null ? this.footerBackItem : this.blankItem);

            this.setItem(-this.nextPageIndex, null);
            this.clearActions(-this.nextPageIndex);
        }
    }

    @Override
    protected @NotNull Inventory generateInventory(@NotNull GuiManager manager) {
        this.guiManager = manager;

        // calculate pages here
        this.rowsPerPage = this.useHeader ? 4 : 5;
        this.maxCellSlot = this.cellItems.keySet().stream().max(Integer::compare).orElse(0) + 1;
        int maxRows = (int) Math.ceil(this.maxCellSlot / 9.);
        this.pages = (int) Math.max(1, Math.ceil(maxRows / (double) this.rowsPerPage));
        this.setRows(maxRows + (this.useHeader ? 1 : 0));

        // create inventory view
        createInventory();

        // populate and return the display inventory
        setPage(Math.min(this.page, this.pages));
        update();

        return this.inventory;
    }

    @Override
    protected void createInventory() {
        final int cells = this.rows * 9;

        this.inventory = Bukkit.getServer().createInventory(new GuiHolder(this.guiManager, this), cells,
                this.title == null ? "" : trimTitle(this.title));
    }

    @Override
    public void update() {
        if (this.inventory == null) {
            return;
        }

        // calculate pages here
        this.rowsPerPage = this.useHeader ? 4 : 5;
        this.maxCellSlot = (this.cellItems.isEmpty() ? 0 : this.cellItems.keySet().stream().max(Integer::compare).get()) + 1;
        int maxRows = Math.max((this.useHeader ? 1 : 0), (int) Math.ceil(this.maxCellSlot / 9.));
        this.pages = (int) Math.ceil(maxRows / this.rowsPerPage);

        // create a new inventory if needed
        List<Player> toUpdate = null;
        if (Math.min(54, (maxRows + (this.useHeader ? 1 : 0)) * 9) != this.inventory.getSize()) {
            toUpdate = getPlayers();

            this.setRows(maxRows + (this.useHeader ? 1 : 0));

            createInventory();
        }

        // populate header
        if (this.useHeader) {
            for (int i = 0; i < 9; ++i) {
                final ItemStack item = this.cellItems.get(i);

                this.inventory.setItem(i, item != null ? item : (this.headerBackItem != null ? this.headerBackItem : this.blankItem));
            }
        }

        // the last row is dedicated to pagination
        final int cells = this.rows * 9;
        for (int i = cells - 9; i < cells; ++i) {
            this.inventory.setItem(i, this.footerBackItem != null ? this.footerBackItem : this.blankItem);
        }

        // fill out the rest of the page
        showPage();

        // did we need to change the display window size?
        if (toUpdate != null) {
            // whoopsie!
            exit();
            toUpdate.forEach(player -> this.guiManager.showGUI(player, this));
        }
    }

    @Override
    protected boolean onClick(@NotNull GuiManager manager, @NotNull Player player, @NotNull Inventory inventory, InventoryClickEvent event) {
        int cell = event.getSlot();
        Map<ClickType, Clickable> conditionals;

        if (this.useHeader && cell < 9) {
            conditionals = this.conditionalButtons.get(cell);
        } else if (cell >= (this.rows - 1) * 9) {
            // footer row
            conditionals = this.conditionalButtons.get(cell - (this.rows * 9));
        } else {
            int cellIndex = this.page == 1 ? cell : cell + (this.page - 1) * this.rowsPerPage * 9;
            conditionals = this.conditionalButtons.get(cellIndex);
        }

        Clickable button;
        if (conditionals != null
                && ((button = conditionals.get(event.getClick())) != null || (button = conditionals.get(null)) != null)) {
            button.onClick(new GuiClickEvent(manager, this, player, event, cell, true));
        } else {
            // no event for this button
            return false;
        }

        return true;
    }
}
