package com.songoda.core.gui;

import com.songoda.core.chat.AdventureUtils;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.gui.events.GuiClickEvent;
import com.songoda.core.gui.events.GuiCloseEvent;
import com.songoda.core.gui.events.GuiDropItemEvent;
import com.songoda.core.gui.events.GuiOpenEvent;
import com.songoda.core.gui.events.GuiPageEvent;
import com.songoda.core.gui.methods.Clickable;
import com.songoda.core.gui.methods.Closable;
import com.songoda.core.gui.methods.Droppable;
import com.songoda.core.gui.methods.Openable;
import com.songoda.core.gui.methods.Pagable;
import com.songoda.core.utils.ItemUtils;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TODO: animated buttons
 */
public class Gui {
    protected Inventory inventory;
    protected String title;
    protected GuiType inventoryType = GuiType.STANDARD;
    protected int rows, page = 1, pages = 1;
    protected boolean acceptsItems = false;
    protected boolean allowDropItems = true;
    protected boolean allowClose = true;
    protected final Map<Integer, Boolean> unlockedCells = new HashMap<>();
    protected final Map<Integer, ItemStack> cellItems = new HashMap<>();
    protected final Map<Integer, Map<ClickType, Clickable>> conditionalButtons = new HashMap<>();
    protected ItemStack blankItem = GuiUtils.getBorderGlassItem();
    protected int nextPageIndex = -1, prevPageIndex = -1;
    protected ItemStack nextPageItem, prevPageItem;
    protected ItemStack nextPage, prevPage;
    protected Gui parent = null;
    protected static final ItemStack AIR = new ItemStack(Material.AIR);

    protected GuiManager guiManager;
    protected boolean open = false;
    protected Clickable defaultClicker = null;
    protected Clickable privateDefaultClicker = null;
    protected Openable opener = null;
    protected Closable closer = null;
    protected Droppable dropper = null;
    protected Pagable pager = null;
    protected XSound defaultSound = XSound.UI_BUTTON_CLICK;

    public Gui() {
        this.rows = 3;
    }

    public Gui(@NotNull GuiType type) {
        this.inventoryType = type;


        switch (type) {
            case HOPPER:
            case DISPENSER:
            case FURNACE:
                this.rows = 1;
                break;
            default:
                this.rows = 3;
                break;
        }
    }

    public Gui(@Nullable Gui parent) {
        this.parent = parent;
    }

    public Gui(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
    }

    public Gui(int rows, @Nullable Gui parent) {
        this.parent = parent;
        this.rows = Math.max(1, Math.min(6, rows));
    }

    @NotNull
    public List<Player> getPlayers() {
        if (this.inventory == null) {
            return Collections.emptyList();
        }

        return this.inventory.getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .collect(Collectors.toList());
    }

    public boolean isOpen() {
        // double check
        if (this.inventory != null && this.inventory.getViewers().isEmpty()) {
            this.open = false;
        }

        return this.open;
    }

    public boolean getAcceptsItems() {
        return this.acceptsItems;
    }

    public Gui setAcceptsItems(boolean acceptsItems) {
        this.acceptsItems = acceptsItems;
        return this;
    }

    /**
     * If this is true, then items in the player's cursor when the GUI is closed
     * will be cleared
     */
    public boolean getAllowDrops() {
        return this.allowDropItems;
    }

    /**
     * Set if items in the player's cursor will be cleared when the GUI is
     * closed
     */
    public Gui setAllowDrops(boolean allow) {
        this.allowDropItems = allow;
        return this;
    }

    public boolean getAllowClose() {
        return this.allowClose;
    }

    public Gui setAllowClose(boolean allow) {
        this.allowClose = allow;
        return this;
    }

    /**
     * Close the GUI without calling onClose() and without opening any parent
     * GUIs
     */
    public void exit() {
        this.allowClose = true;
        this.open = false;

        this.inventory.getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .collect(Collectors.toList())
                .forEach(Player::closeInventory);
    }

    /**
     * Close the GUI as if the player closed it normally
     */
    public void close() {
        this.allowClose = true;

        this.inventory.getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .collect(Collectors.toList())
                .forEach(Player::closeInventory);
    }

    @NotNull
    public GuiType getType() {
        return this.inventoryType;
    }

    @NotNull
    public Gui setUnlocked(int cell) {
        this.unlockedCells.put(cell, true);
        return this;
    }

    @NotNull
    public Gui setUnlocked(int row, int col) {
        final int cell = col + row * this.inventoryType.columns;
        this.unlockedCells.put(cell, true);

        return this;
    }

    @NotNull
    public Gui setUnlockedRange(int cellFirst, int cellLast) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            this.unlockedCells.put(cell, true);
        }

        return this;
    }

    @NotNull
    public Gui setUnlockedRange(int cellFirst, int cellLast, boolean open) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            this.unlockedCells.put(cell, open);
        }

        return this;
    }

    @NotNull
    public Gui setUnlockedRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast) {
        final int last = cellColLast + cellRowLast * this.inventoryType.columns;

        for (int cell = cellColFirst + cellRowFirst * this.inventoryType.columns; cell <= last; ++cell) {
            this.unlockedCells.put(cell, true);
        }

        return this;
    }

    @NotNull
    public Gui setUnlockedRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, boolean open) {
        final int last = cellColLast + cellRowLast * this.inventoryType.columns;

        for (int cell = cellColFirst + cellRowFirst * this.inventoryType.columns; cell <= last; ++cell) {
            this.unlockedCells.put(cell, open);
        }

        return this;
    }

    @NotNull
    public Gui setUnlocked(int cell, boolean open) {
        this.unlockedCells.put(cell, open);
        return this;
    }

    @NotNull
    public Gui setUnlocked(int row, int col, boolean open) {
        final int cell = col + row * this.inventoryType.columns;
        this.unlockedCells.put(cell, open);

        return this;
    }

    @NotNull
    public Gui setTitle(String title) {
        if (title == null) {
            title = "";
        }

        if (!title.equals(this.title)) {
            this.title = title;

            if (this.inventory != null) {
                // update active inventory
                List<Player> toUpdate = getPlayers();
                boolean isAllowClose = this.allowClose;
                exit();

                Inventory oldInv = this.inventory;
                createInventory();
                this.inventory.setContents(oldInv.getContents());

                toUpdate.forEach(player -> player.openInventory(this.inventory));

                this.allowClose = isAllowClose;
            }
        }

        return this;
    }

    public Gui setTitle(Component title) {
        setTitle(AdventureUtils.toLegacy(title));
        return this;
    }

    public int getRows() {
        return this.rows;
    }

    @NotNull
    public Gui setRows(int rows) {
        switch (this.inventoryType) {
            case HOPPER:
            case DISPENSER:
            case FURNACE:
                break;
            default:
                this.rows = Math.max(1, Math.min(6, rows));
                break;
        }

        return this;
    }

    @NotNull
    public Gui setDefaultAction(@Nullable Clickable action) {
        this.defaultClicker = action;
        return this;
    }

    @NotNull
    protected Gui setPrivateDefaultAction(@Nullable Clickable action) {
        this.privateDefaultClicker = action;
        return this;
    }

    @NotNull
    public Gui setDefaultItem(@Nullable ItemStack item) {
        this.blankItem = item;
        return this;
    }

    @Nullable
    public ItemStack getDefaultItem() {
        return this.blankItem;
    }

    @Nullable
    public ItemStack getItem(int cell) {
        if (this.inventory != null && this.unlockedCells.getOrDefault(cell, false)) {
            return this.inventory.getItem(cell);
        }

        return this.cellItems.get(cell);
    }

    @Nullable
    public ItemStack getItem(int row, int col) {
        final int cell = col + row * this.inventoryType.columns;

        if (this.inventory != null && this.unlockedCells.getOrDefault(cell, false)) {
            return this.inventory.getItem(cell);
        }

        return this.cellItems.get(cell);
    }

    @NotNull
    public Gui setItem(int cell, @Nullable ItemStack item) {
        this.cellItems.put(cell, item);

        if (this.inventory != null && cell >= 0 && cell < this.inventory.getSize()) {
            this.inventory.setItem(cell, item);
        }

        return this;
    }

    @NotNull
    public Gui setItem(int row, int col, @Nullable ItemStack item) {
        final int cell = col + row * this.inventoryType.columns;
        return setItem(cell, item);
    }

    @NotNull
    public Gui mirrorFill(int row, int col, boolean mirrorRow, boolean mirrorCol, ItemStack item) {
        setItem(row, col, item);

        if (mirrorRow) {
            setItem(this.rows - row - 1, col, item);
        }

        if (mirrorCol) {
            setItem(row, 8 - col, item);
        }

        if (mirrorRow && mirrorCol) {
            setItem(this.rows - row - 1, 8 - col, item);
        }

        return this;
    }

    @NotNull
    public Gui highlightItem(int cell) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, ItemUtils.addGlow(item));
        }

        return this;
    }

    @NotNull
    public Gui highlightItem(int row, int col) {
        final int cell = col + row * this.inventoryType.columns;

        return highlightItem(cell);
    }

    @NotNull
    public Gui removeHighlight(int cell) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, ItemUtils.removeGlow(item));
        }

        return this;
    }

    @NotNull
    public Gui removeHighlight(int row, int col) {
        final int cell = col + row * this.inventoryType.columns;
        return removeHighlight(cell);
    }

    @NotNull
    public Gui updateItemLore(int row, int col, @NotNull String... lore) {
        return updateItemLore(col + row * this.inventoryType.columns, lore);
    }

    @NotNull
    public Gui updateItemLore(int cell, @NotNull String... lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItemLore(item, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItemLore(int row, int col, @Nullable List<String> lore) {
        return updateItemLore(col + row * this.inventoryType.columns, lore);
    }

    @NotNull
    public Gui updateItemLore(int cell, @Nullable List<String> lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItemLore(item, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItemName(int row, int col, @Nullable String name) {
        return updateItemName(col + row * this.inventoryType.columns, name);
    }

    @NotNull
    public Gui updateItemName(int cell, @Nullable String name) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItemName(item, name));
        }

        return this;
    }

    @NotNull
    public Gui updateItem(int row, int col, @Nullable String name, @NotNull String... lore) {
        return updateItem(col + row * this.inventoryType.columns, name, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @Nullable String name, @NotNull String... lore) {
        return updateItem(cell, name, Arrays.asList(lore));
    }

    @NotNull
    public Gui updateItem(int row, int col, @Nullable String name, @Nullable List<String> lore) {
        return updateItem(col + row * this.inventoryType.columns, name, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @NotNull String name, @Nullable List<String> lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItem(item, name, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItem(int row, int col, @NotNull ItemStack itemTo, @Nullable String title, @NotNull String... lore) {
        return updateItem(col + row * this.inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @NotNull ItemStack itemTo, @Nullable String title, @NotNull String... lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItem(item, itemTo, title, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItem(int row, int col, @NotNull XMaterial itemTo, @Nullable String title, @NotNull String... lore) {
        return updateItem(col + row * this.inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @NotNull XMaterial itemTo, @Nullable String title, @Nullable String... lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItem(item, itemTo, title, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItem(int row, int col, @NotNull ItemStack itemTo, @Nullable String title, @Nullable List<String> lore) {
        return updateItem(col + row * this.inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @NotNull ItemStack itemTo, @Nullable String title, @Nullable List<String> lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItem(item, itemTo, title, lore));
        }

        return this;
    }

    @NotNull
    public Gui updateItem(int row, int col, @NotNull XMaterial itemTo, @Nullable String title, @Nullable List<String> lore) {
        return updateItem(col + row * this.inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(int cell, @NotNull XMaterial itemTo, @Nullable String title, @Nullable List<String> lore) {
        ItemStack item = this.cellItems.get(cell);

        if (item != null && item.getType() != Material.AIR) {
            setItem(cell, GuiUtils.updateItem(item, itemTo, title, lore));
        }

        return this;
    }

    @NotNull
    public Gui setAction(int cell, @Nullable Clickable action) {
        setConditional(cell, null, action);
        return this;
    }

    @NotNull
    public Gui setAction(int row, int col, @Nullable Clickable action) {
        setConditional(col + row * this.inventoryType.columns, null, action);
        return this;
    }

    @NotNull
    public Gui setAction(int cell, @Nullable ClickType type, @Nullable Clickable action) {
        setConditional(cell, type, action);
        return this;
    }

    @NotNull
    public Gui setAction(int row, int col, @Nullable ClickType type, @Nullable Clickable action) {
        setConditional(col + row * this.inventoryType.columns, type, action);
        return this;
    }

    @NotNull
    public Gui setActionForRange(int cellFirst, int cellLast, @Nullable Clickable action) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            setConditional(cell, null, action);
        }

        return this;
    }

    @NotNull
    public Gui setActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, @Nullable Clickable action) {
        final int last = cellColLast + cellRowLast * this.inventoryType.columns;

        for (int cell = cellColFirst + cellRowFirst * this.inventoryType.columns; cell <= last; ++cell) {
            setConditional(cell, null, action);
        }

        return this;
    }

    @NotNull
    public Gui setActionForRange(int cellFirst, int cellLast, @Nullable ClickType type, @Nullable Clickable action) {
        for (int cell = cellFirst; cell <= cellLast; ++cell) {
            setConditional(cell, type, action);
        }

        return this;
    }

    @NotNull
    public Gui setActionForRange(int cellRowFirst, int cellColFirst, int cellRowLast, int cellColLast, @Nullable ClickType type, @Nullable Clickable action) {
        final int last = cellColLast + cellRowLast * this.inventoryType.columns;

        for (int cell = cellColFirst + cellRowFirst * this.inventoryType.columns; cell <= last; ++cell) {
            setConditional(cell, type, action);
        }

        return this;
    }

    @NotNull
    public Gui clearActions(int cell) {
        this.conditionalButtons.remove(cell);
        return this;
    }

    @NotNull
    public Gui clearActions(int row, int col) {
        return clearActions(col + row * this.inventoryType.columns);
    }

    @NotNull
    public Gui setButton(int cell, ItemStack item, @Nullable Clickable action) {
        setItem(cell, item);
        setConditional(cell, null, action);

        return this;
    }

    @NotNull
    public Gui setButton(int row, int col, @Nullable ItemStack item, @Nullable Clickable action) {
        final int cell = col + row * this.inventoryType.columns;

        setItem(cell, item);
        setConditional(cell, null, action);

        return this;
    }

    @NotNull
    public Gui setButton(int cell, @Nullable ItemStack item, @Nullable ClickType type, @Nullable Clickable action) {
        setItem(cell, item);
        setConditional(cell, type, action);

        return this;
    }

    @NotNull
    public Gui setButton(int row, int col, @Nullable ItemStack item, @Nullable ClickType type, @Nullable Clickable action) {
        final int cell = col + row * this.inventoryType.columns;

        setItem(cell, item);
        setConditional(cell, type, action);

        return this;
    }

    protected void setConditional(int cell, @Nullable ClickType type, @Nullable Clickable action) {
        Map<ClickType, Clickable> conditionals = this.conditionalButtons.computeIfAbsent(cell, k -> new HashMap<>());
        conditionals.put(type, action);
    }

    @NotNull
    public Gui setOnOpen(@Nullable Openable action) {
        this.opener = action;
        return this;
    }

    @NotNull
    public Gui setOnClose(@Nullable Closable action) {
        this.closer = action;
        return this;
    }

    @NotNull
    public Gui setOnDrop(@Nullable Droppable action) {
        this.dropper = action;
        return this;
    }

    @NotNull
    public Gui setOnPage(@Nullable Pagable action) {
        this.pager = action;
        return this;
    }

    public Gui setNextPage(ItemStack item) {
        this.nextPage = item;
        return this;
    }

    public Gui setPrevPage(ItemStack item) {
        this.prevPage = item;
        return this;
    }

    public void reset() {
        if (this.inventory != null) {
            this.inventory.clear();
        }

        setActionForRange(0, 53, null);
        this.cellItems.clear();
        update();
    }

    @NotNull
    public Gui setNextPage(int cell, @NotNull ItemStack item) {
        this.nextPageItem = this.cellItems.get(cell);
        this.nextPageIndex = cell;
        this.nextPage = item;

        if (this.page < this.pages) {
            setButton(this.nextPageIndex, this.nextPage, ClickType.LEFT, (event) -> this.nextPage());
        }

        return this;
    }

    @NotNull
    public Gui setNextPage(int row, int col, @NotNull ItemStack item) {
        return setNextPage(col + row * this.inventoryType.columns, item);
    }

    @NotNull
    public Gui setPrevPage(int cell, @NotNull ItemStack item) {
        this.prevPageItem = this.cellItems.get(cell);
        this.prevPageIndex = cell;
        this.prevPage = item;

        if (this.page > 1) {
            setButton(this.prevPageIndex, this.prevPage, ClickType.LEFT, (event) -> this.prevPage());
        }

        return this;
    }

    @NotNull
    public Gui setPrevPage(int row, int col, @NotNull ItemStack item) {
        return setPrevPage(col + row * this.inventoryType.columns, item);
    }

    public void setPages(int pages) {
        this.pages = Math.max(1, pages);

        if (this.page > pages) {
            setPage(pages);
        }
    }

    public void setPage(int page) {
        int lastPage = this.page;
        this.page = Math.max(1, Math.min(this.pages, page));

        if (this.pager != null && this.page != lastPage) {
            this.pager.onPageChange(new GuiPageEvent(this, this.guiManager, lastPage, page));

            // page markers
            updatePageNavigation();
        }
    }

    public void changePage(int direction) {
        int lastPage = this.page;
        this.page = Math.max(1, Math.min(this.pages, this.page + direction));

        if (this.pager != null && this.page != lastPage) {
            this.pager.onPageChange(new GuiPageEvent(this, this.guiManager, lastPage, this.page));

            // page markers
            updatePageNavigation();
        }
    }

    public void nextPage() {
        if (this.page < this.pages) {
            int lastPage = this.page;
            ++this.page;

            // page switch events
            if (this.pager != null) {
                this.pager.onPageChange(new GuiPageEvent(this, this.guiManager, lastPage, this.page));

                // page markers
                updatePageNavigation();

                // push new inventory to the view inventory
                // shouldn't be needed since adding inventory update to setItem
                //update();
            }
        }
    }

    public void prevPage() {
        if (this.page > 1) {
            int lastPage = this.page;
            --this.page;

            if (this.pager != null) {
                this.pager.onPageChange(new GuiPageEvent(this, this.guiManager, lastPage, this.page));

                // page markers
                updatePageNavigation();

                // push new inventory to the view inventory
                // shouldn't be needed since adding inventory update to setItem
                //update();
            }
        }
    }

    protected void updatePageNavigation() {
        if (this.prevPage != null) {
            if (this.page > 1) {
                this.setButton(this.prevPageIndex, this.prevPage, ClickType.LEFT, (event) -> this.prevPage());
            } else {
                this.setItem(this.prevPageIndex, this.prevPageItem);
                this.clearActions(this.prevPageIndex);
            }
        }

        if (this.nextPage != null) {
            if (this.pages > 1 && this.page != this.pages) {
                this.setButton(this.nextPageIndex, this.nextPage, ClickType.LEFT, (event) -> this.nextPage());
            } else {
                this.setItem(this.nextPageIndex, this.nextPageItem);
                this.clearActions(this.nextPageIndex);
            }
        }
    }

    @NotNull
    protected Inventory getOrCreateInventory(@NotNull GuiManager manager) {
        return this.inventory != null ? this.inventory : generateInventory(manager);
    }

    @NotNull
    protected Inventory generateInventory(@NotNull GuiManager manager) {
        this.guiManager = manager;
        final int cells = this.rows * this.inventoryType.columns;

        createInventory();

        for (int i = 0; i < cells; ++i) {
            final ItemStack item = this.cellItems.get(i);
            this.inventory.setItem(i, item != null ? item : (this.unlockedCells.getOrDefault(i, false) ? AIR : this.blankItem));
        }

        return this.inventory;
    }

    protected void createInventory() {
        final InventoryType t = this.inventoryType == null ? InventoryType.CHEST : this.inventoryType.type;

        switch (t) {
            case DISPENSER:
            case HOPPER:
            case FURNACE:
                this.inventory = new GuiHolder(this.guiManager, this).newInventory(t,
                        this.title == null ? "" : trimTitle(this.title));
                break;
            default:
                this.inventory = new GuiHolder(this.guiManager, this).newInventory(this.rows * 9,
                        this.title == null ? "" : trimTitle(this.title));
                break;
        }
    }

    @Nullable
    public Gui getParent() {
        return this.parent;
    }

    public void update() {
        if (this.inventory == null) {
            return;
        }

        final int cells = this.rows * this.inventoryType.columns;
        for (int i = 0; i < cells; ++i) {
            final ItemStack item = this.cellItems.get(i);
            this.inventory.setItem(i, item != null ? item : (this.unlockedCells.getOrDefault(i, false) ? AIR : this.blankItem));
        }
    }

    protected static String trimTitle(String title) {
        if (title == null) {
            return "";
        }

        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9) && title.length() > 32) {
            return title.charAt(30) == '§' ? title.substring(0, 30) : title.substring(0, 31);
        }

        return title;
    }

    protected boolean onClickOutside(@NotNull GuiManager manager, @NotNull Player player, @NotNull InventoryClickEvent event) {
        return this.dropper == null || this.dropper.onDrop(new GuiDropItemEvent(manager, this, player, event));
    }

    protected boolean onClick(@NotNull GuiManager manager, @NotNull Player player, @NotNull Inventory inventory, @NotNull InventoryClickEvent event) {
        final int cell = event.getSlot();
        Map<ClickType, Clickable> conditionals = this.conditionalButtons.get(cell);

        Clickable button;
        if (conditionals != null
                && ((button = conditionals.get(event.getClick())) != null || (button = conditionals.get(null)) != null)) {
            button.onClick(new GuiClickEvent(manager, this, player, event, cell, true));
        } else {
            // no event for this button
            if (this.defaultClicker != null) {
                // this is a default action, not a triggered action
                this.defaultClicker.onClick(new GuiClickEvent(manager, this, player, event, cell, true));
            }

            if (this.privateDefaultClicker != null) {
                // this is a private default action, not a triggered action
                this.privateDefaultClicker.onClick(new GuiClickEvent(manager, this, player, event, cell, true));
            }

            return false;
        }

        return true;
    }

    protected boolean onClickPlayerInventory(@NotNull GuiManager manager, @NotNull Player player, @NotNull Inventory openInv, InventoryClickEvent event) {
        // no events for this yet
        return false;
    }

    public void onOpen(@NotNull GuiManager manager, @NotNull Player player) {
        this.open = true;
        this.guiManager = manager;

        if (this.opener != null) {
            this.opener.onOpen(new GuiOpenEvent(manager, this, player));
        }
    }

    public void onClose(@NotNull GuiManager manager, @NotNull Player player) {
        if (!this.allowClose) {
            manager.showGUI(player, this);
            return;
        }

        boolean showParent = this.open && this.parent != null;

        if (this.closer != null) {
            this.closer.onClose(new GuiCloseEvent(manager, this, player));
        }

        if (showParent) {
            manager.showGUI(player, this.parent);
        }
    }

    public XSound getDefaultSound() {
        return this.defaultSound;
    }

    public void setDefaultSound(XSound sound) {
        this.defaultSound = sound;
    }
}
