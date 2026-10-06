package com.songoda.core.gui;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vinject.config.ConfigurationSection;
import com.songoda.core.SongodaPlugin;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Paginated GUI; inventory title and items use {@link AdventureUtils} and Paper's native Adventure API like {@link Gui}.
 */
public class PaginatedGui implements GuiHolder {
    private static final UUID UID = UUID.randomUUID();

    private final int rows;
    private final Inventory inventory;
    private final List<Player> openers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoUpdatePending = new AtomicBoolean();
    private final Map<Integer, List<GuiItem>> pages = new LinkedHashMap<>();
    // Static items that don't change with pagination
    private final List<GuiItem> staticItems = new ArrayList<>();
    private final Component baseTitle;
    private @Getter Consumer<InventoryClickEvent> onGlobalClick;
    private @Getter Consumer<InventoryClickEvent> onTopClick;
    private @Getter Consumer<InventoryClickEvent> onBottomClick;
    private @Getter Consumer<InventoryDragEvent> onGlobalDrag;
    private @Getter Consumer<InventoryDragEvent> onTopDrag;
    private @Getter Consumer<InventoryDragEvent> onBottomDrag;
    private @Getter Consumer<InventoryCloseEvent> onClose;
    private boolean cancelClick = true;
    private Gui previousGui = null;
    private int currentPage = 0;
    private int itemsPerPage;
    // Button customization
    private Supplier<ItemStack> previousButtonSupplier;
    private Supplier<ItemStack> nextButtonSupplier;
    // Page indicator
    private Supplier<ItemStack> pageIndicatorSupplier;
    private int pageIndicatorSlot = -1;

    // Pattern for item placement
    private int[] slotPattern;
    private boolean usePattern = false;

    public PaginatedGui(String title, int rows) {
        this.rows = rows;
        this.baseTitle = AdventureUtils.formatComponent(title);
        this.inventory = AdventureUtils.createInventory(this, rows, baseTitle);
        this.itemsPerPage = (rows - 1) * 9; // Reserve bottom row for navigation

        // Default button suppliers
        this.previousButtonSupplier = () -> {
            ItemStack item = new ItemStack(Material.ARROW);
            AdventureUtils.formatItemName(item, "§aPrevious Page");
            return item;
        };

        this.nextButtonSupplier = () -> {
            ItemStack item = new ItemStack(Material.ARROW);
            AdventureUtils.formatItemName(item, "§aNext Page");
            return item;
        };

        // Default page indicator
        this.pageIndicatorSupplier = () -> {
            ItemStack item = new ItemStack(Material.BOOK);
            AdventureUtils.formatItemName(item, "§7Page " + (currentPage + 1) + "/" + Math.max(1, pages.size()));
            return item;
        };
        this.pageIndicatorSlot = (rows - 1) * 9 + 4; // Center of bottom row
    }

    public PaginatedGui(Component title, int rows) {
        this.rows = rows;
        this.baseTitle = title;
        this.inventory = AdventureUtils.createInventory(this, rows, title);
        this.itemsPerPage = (rows - 1) * 9; // Reserve bottom row for navigation

        // Default button suppliers
        this.previousButtonSupplier = () -> {
            ItemStack item = new ItemStack(Material.ARROW);
            AdventureUtils.formatItemName(item, "§aPrevious Page");
            return item;
        };

        this.nextButtonSupplier = () -> {
            ItemStack item = new ItemStack(Material.ARROW);
            AdventureUtils.formatItemName(item, "§aNext Page");
            return item;
        };

        // Default page indicator
        this.pageIndicatorSupplier = () -> {
            ItemStack item = new ItemStack(Material.BOOK);
            AdventureUtils.formatItemName(item, "§7Page " + (currentPage + 1) + "/" + Math.max(1, pages.size()));
            return item;
        };
        this.pageIndicatorSlot = (rows - 1) * 9 + 4; // Center of bottom row
    }

    /**
     * Sets the slot for the page indicator item
     *
     * @param slot the slot to place the page indicator
     * @return this PaginatedGui instance
     */
    public PaginatedGui setPageIndicatorSlot(int slot) {
        this.pageIndicatorSlot = slot;
        return this;
    }

    /**
     * Sets a custom page indicator supplier
     *
     * @param supplier the supplier for the page indicator item
     * @return this PaginatedGui instance
     */
    public PaginatedGui setPageIndicatorSupplier(Supplier<ItemStack> supplier) {
        this.pageIndicatorSupplier = supplier;
        return this;
    }

    /**
     * Adds a static item at specific coordinates that won't be affected by pagination
     *
     * @param item the item to add
     * @return this PaginatedGui instance
     */
    public PaginatedGui addStaticItem(GuiItem item) {
        if (item.getX() == -1 || item.getY() == -1) {
            throw new IllegalArgumentException("Static items must have x and y coordinates");
        }

        // Remove any existing static item in the same position
        staticItems.removeIf(i -> i.getX() == item.getX() && i.getY() == item.getY());
        staticItems.add(item);
        inventory.setItem(item.getX() + item.getY() * 9, item.getItem());
        return this;
    }

    /**
     * Adds a static item at specific coordinates that won't be affected by pagination
     *
     * @param item  the item to add
     * @param event the click handler
     * @param x     the x position (0-8)
     * @param y     the y position (0-rows)
     * @return this PaginatedGui instance
     */
    public PaginatedGui addStaticItem(ItemStack item, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event, int x, int y) {
        return addStaticItem(new GuiItem(item, event, x, y));
    }

    /**
     * Adds a static item at specific coordinates that won't be affected by pagination
     *
     * @param item the item to add
     * @param x    the x position (0-8)
     * @param y    the y position (0-rows)
     * @return this PaginatedGui instance
     */
    public PaginatedGui addStaticItem(ItemStack item, int x, int y) {
        return addStaticItem(new GuiItem(item, x, y));
    }

    public PaginatedGui addStaticItem(Consumer<ItemStackBuilder> itemStackBuilder, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event, int x, int y) {
        return addStaticItem(new GuiItem(itemStackBuilder, event, x, y));
    }

    public PaginatedGui fetchFills(ConfigurationSection config) {
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

    public PaginatedGui fillEmpty(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");
        ItemStack clonedItem = item.clone();

        for (int y = 0; y < rows; y++) { // Include all rows
            for (int x = 0; x < 9; x++) {
                int slot = x + y * 9;

                // Skip navigation button positions in bottom row
                if (y == rows - 1) {
                    // Skip nav buttons and page indicator
                    if (x == 3 || x == 5 || slot == pageIndicatorSlot || (x == 0 && previousGui != null)) {
                        continue;
                    }
                }

                // Check if slot is in pattern and already has an item
                boolean shouldSkip = false;

                // Skip slots used in pattern with items
                if (usePattern) {
                    for (int patternSlot : slotPattern) {
                        if (patternSlot == slot) {
                            List<GuiItem> pageItems = pages.getOrDefault(currentPage, new ArrayList<>());
                            int patternIndex = -1;
                            for (int i = 0; i < slotPattern.length; i++) {
                                if (slotPattern[i] == slot) {
                                    patternIndex = i;
                                    break;
                                }
                            }
                            if (patternIndex < pageItems.size()) {
                                shouldSkip = true;
                                break;
                            }
                        }
                    }
                }

                // Skip slots with static items
                if (staticItems.stream().anyMatch(i -> (i.getX() + i.getY() * 9) == slot)) {
                    shouldSkip = true;
                }

                if (!shouldSkip) {
                    addStaticItem(clonedItem.clone(), x, y);
                }
            }
        }
        return this;
    }

    public PaginatedGui fillEmpty(Material material) {
        return fillEmpty(new ItemStack(material));
    }

    public PaginatedGui fillBorder(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");
        ItemStack clonedItem = item.clone();

        // Fill first row
        for (int x = 0; x < 9; x++) {
            addStaticItem(clonedItem.clone(), x, 0);
        }

        // Fill first column and last column
        for (int y = 1; y < rows - 1; y++) {
            addStaticItem(clonedItem.clone(), 0, y);
            addStaticItem(clonedItem.clone(), 8, y);
        }

        // Fill last row except navigation buttons
        int lastRow = rows - 1;
        for (int x = 0; x < 9; x++) {
            // Skip navigation button positions
            if (x == 3 || x == 4 || x == 5 || (x == 0 && previousGui != null)) continue;

            addStaticItem(clonedItem.clone(), x, lastRow);
        }
        return this;
    }

    public PaginatedGui fillBorder(Material material) {
        return fillBorder(new ItemStack(material));
    }

    public PaginatedGui fillBottom(ItemStack item) {
        AdventureUtils.formatItemName(item, " ");
        ItemStack clonedItem = item.clone();

        int lastRow = rows - 1;
        for (int x = 0; x < 9; x++) {
            // Skip navigation button positions
            if (x == 3 || x == 4 || x == 5 || (x == 0 && previousGui != null)) continue;

            addStaticItem(clonedItem.clone(), x, lastRow);
        }
        return this;
    }

    public PaginatedGui fillBottom(Material material) {
        return fillBottom(new ItemStack(material));
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(boolean clear) {
        if (clear) {
            renderPage();
        } else {
            // Update paginated items if needed
            List<GuiItem> items = pages.getOrDefault(currentPage, new ArrayList<>());

            if (usePattern) {
                for (int i = 0; i < Math.min(items.size(), slotPattern.length); i++) {
                    GuiItem item = items.get(i);
                    if (item.shouldUpdate() || item.getState() == State.AUTO_UPDATE) {
                        item.updateItem();
                        inventory.setItem(slotPattern[i], item.getItem());
                    }
                }
            } else {
                for (int i = 0; i < Math.min(items.size(), itemsPerPage); i++) {
                    GuiItem item = items.get(i);
                    if (item.shouldUpdate() || item.getState() == State.AUTO_UPDATE) {
                        item.updateItem();
                        int x = i % 9;
                        int y = i / 9;
                        inventory.setItem(x + y * 9, item.getItem());
                    }
                }
            }

            // Update static items if needed
            for (GuiItem item : staticItems) {
                if (item.shouldUpdate() || item.getState() == State.AUTO_UPDATE) {
                    item.updateItem();
                    inventory.setItem(item.getX() + item.getY() * 9, item.getItem());
                }
            }

            // Re-add navigation buttons and page indicator
            renderNavigationButtons();
        }
    }

    public void reDrawIfNeeded() {
        boolean needsRedraw = false;

        // Check paginated items
        List<GuiItem> items = pages.getOrDefault(currentPage, new ArrayList<>());
        Iterator<GuiItem> it = items.iterator();
        while (it.hasNext()) {
            GuiItem item = it.next();
            if (item.shouldUpdate()) {
                needsRedraw = true;
                item.updateItem();
                if (item.getItem().getType() == Material.AIR) {
                    it.remove();
                }
            }
        }

        // Check static items
        Iterator<GuiItem> staticIt = staticItems.iterator();
        while (staticIt.hasNext()) {
            GuiItem item = staticIt.next();
            if (item.shouldUpdate()) {
                needsRedraw = true;
                item.updateItem();
                if (item.getItem().getType() == Material.AIR) {
                    staticIt.remove();
                }
            }
        }

        if (needsRedraw) {
            refresh();
        }
    }

    public void reopen(Player player) {
        show(player);
    }

    public void reopenAll() {
        for (Player player : new ArrayList<>(openers)) {
            show(player);
        }
    }

    /**
     * Sets a pattern of slots where items should be placed.
     * This overrides the default sequential placement.
     *
     * @param slots Array of slot indices where items should be placed
     * @return this PaginatedGui instance
     */
    public PaginatedGui setSlotPattern(int... slots) {
        if (slots != null && slots.length > 0) {
            this.slotPattern = Arrays.copyOf(slots, slots.length);
            this.usePattern = true;
            this.itemsPerPage = slots.length; // Items per page is now determined by pattern size

            // Filter out slots from bottom row
            int bottomRowStart = (rows - 1) * 9;
            List<Integer> validSlots = new ArrayList<>();
            for (int slot : slots) {
                if (slot < bottomRowStart) {
                    validSlots.add(slot);
                }
            }

            if (validSlots.size() < slots.length) {
                // Create a new array with only valid slots
                this.slotPattern = new int[validSlots.size()];
                for (int i = 0; i < validSlots.size(); i++) {
                    this.slotPattern[i] = validSlots.get(i);
                }
                this.itemsPerPage = validSlots.size();
            }
        } else {
            this.usePattern = false;
            this.itemsPerPage = (rows - 1) * 9;
        }
        return this;
    }

    public PaginatedGui setPreviousButtonSupplier(Supplier<ItemStack> supplier) {
        this.previousButtonSupplier = supplier;
        return this;
    }

    public PaginatedGui setNextButtonSupplier(Supplier<ItemStack> supplier) {
        this.nextButtonSupplier = supplier;
        return this;
    }

    public void addItem(GuiItem item) {
        // For paginated GUI, we ignore x/y and place items automatically
        item.setX(-1);
        item.setY(-1);

        // Ensure page 0 exists
        if (!pages.containsKey(0)) {
            pages.put(0, new ArrayList<>());
        }

        // Find the target page
        int targetPage = pages.size() - 1; // Start with the last page
        List<GuiItem> currentPageItems = pages.get(targetPage);

        // If the last page is full, create a new page
        if (currentPageItems.size() >= itemsPerPage) {
            targetPage++;
            pages.put(targetPage, new ArrayList<>());
            currentPageItems = pages.get(targetPage);
        }

        // Add the item to the target page
        currentPageItems.add(item);
    }

    public void addItem(ItemStack item, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event) {
        addItem(new GuiItem(item, event, -1, -1));
    }

    public void addItem(ItemStack item) {
        addItem(new GuiItem(item, -1, -1));
    }

    public void addItem(Consumer<ItemStackBuilder> itemStackBuilder, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> event) {
        addItem(new GuiItem(itemStackBuilder, event, -1, -1));
    }

    public void setPage(int page) {
        if (page < 0 || page >= pages.size()) {
            return;
        }

        currentPage = page;
        renderPage();
    }

    public void nextPage() {
        if (currentPage < pages.size() - 1) {
            currentPage++;
            renderPage();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    private void renderPage() {
        // Clear inventory
        inventory.clear();

        // Render current page items
        List<GuiItem> items = pages.getOrDefault(currentPage, new ArrayList<>());

        if (usePattern) {
            // Place items according to pattern
            for (int i = 0; i < Math.min(items.size(), slotPattern.length); i++) {
                GuiItem item = items.get(i);
                inventory.setItem(slotPattern[i], item.updateItem());
            }
        } else {
            // Use default sequential placement
            for (int i = 0; i < Math.min(items.size(), itemsPerPage); i++) {
                GuiItem item = items.get(i);
                int x = i % 9;
                int y = i / 9;
                inventory.setItem(x + y * 9, item.updateItem());
            }
        }

        // Render static items
        for (GuiItem item : staticItems) {
            inventory.setItem(item.getX() + item.getY() * 9, item.updateItem());
        }

        // Add navigation buttons
        renderNavigationButtons();
    }

    private void renderNavigationButtons() {
        int bottomRow = (rows - 1) * 9;
        boolean hasMultiplePages = pages.size() > 1;

        // Previous page button (if not on first page and multiple pages exist)
        if (hasMultiplePages && currentPage > 0) {
            inventory.setItem(bottomRow + 3, previousButtonSupplier.get());
        } else {
            inventory.setItem(bottomRow + 3, null);
        }

        // Next page button (if not on last page and multiple pages exist)
        if (hasMultiplePages && currentPage < pages.size() - 1) {
            inventory.setItem(bottomRow + 5, nextButtonSupplier.get());
        } else {
            inventory.setItem(bottomRow + 5, null);
        }

        // Page indicator (only when multiple pages)
        if (hasMultiplePages && pageIndicatorSlot >= 0 && pageIndicatorSupplier != null) {
            inventory.setItem(pageIndicatorSlot, pageIndicatorSupplier.get());
        } else if (pageIndicatorSlot >= 0) {
            inventory.setItem(pageIndicatorSlot, null);
        }

        // Back button if previous GUI exists
        if (previousGui != null) {
            ItemStack back = new ItemStack(Material.BARRIER);
            AdventureUtils.formatItemName(back, "§cBack");
            inventory.setItem(bottomRow, back);
        }
    }

    private Component getPageTitle() {
        return baseTitle;
    }

    public PaginatedGui setPreviousGui(Gui gui) {
        this.previousGui = gui;
        return this;
    }

    public PaginatedGui show(Player player) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(player)) {
            SchedulerUtils.runEntityTask(SongodaPlugin.getInstance(), player, () -> show(player));
            return this;
        }

        // Ensure at least page 0 exists (avoids NPE when no paginated items added)
        if (!pages.containsKey(0)) {
            pages.put(0, new ArrayList<>());
        }
        renderPage();
        player.openInventory(inventory);

        if (!openers.contains(player)) {
            openers.add(player);
        }

        GuiManager.markAsOpen(this);
        return this;
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int bottomRow = (rows - 1) * 9;

        // Check if clicked on a static item first
        for (GuiItem item : staticItems) {
            int itemSlot = item.getX() + item.getY() * 9;
            if (itemSlot == slot) {
                event.setCancelled(true);
                item.onClick(event, this, item);
                return;
            }
        }

        // Fix for previous button - proper slot check
        if (slot == bottomRow + 3 && currentPage > 0) {
            event.setCancelled(true);
            previousPage();
            return;
        }

        if (slot == bottomRow + 5 && currentPage < pages.size() - 1) {
            event.setCancelled(true);
            nextPage();
            return;
        }

        // Handle back button
        if (slot == bottomRow && previousGui != null) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            close(player);
            previousGui.show(player);
            return;
        }

        // Handle page indicator click (do nothing but cancel)
        if (slot == pageIndicatorSlot) {
            event.setCancelled(true);
            return;
        }

        // Handle paginated item click
        if (slot >= 0 && slot < bottomRow) {
            GuiItem item = getItem(slot);
            if (item != null) {
                event.setCancelled(true);
                item.onClick(event, this, item);
            }
        }
    }

    public PaginatedGui close(Player player) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(player)) {
            if (SongodaPlugin.getInstance().isEnabled()) {
                SchedulerUtils.runEntityTask(SongodaPlugin.getInstance(), player, () -> close(player));
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

    // GuiHolder implementation
    @Override
    public PaginatedGui setOnGlobalClick(Consumer<InventoryClickEvent> onGlobalClick) {
        this.onGlobalClick = onGlobalClick;
        return this;
    }

    @Override
    public PaginatedGui setOnTopClick(Consumer<InventoryClickEvent> onTopClick) {
        this.onTopClick = onTopClick;
        return this;
    }

    @Override
    public PaginatedGui setOnBottomClick(Consumer<InventoryClickEvent> onBottomClick) {
        this.onBottomClick = onBottomClick;
        return this;
    }

    @Override
    public PaginatedGui setOnGlobalDrag(Consumer<InventoryDragEvent> onGlobalDrag) {
        this.onGlobalDrag = onGlobalDrag;
        return this;
    }

    @Override
    public PaginatedGui setOnTopDrag(Consumer<InventoryDragEvent> onTopDrag) {
        this.onTopDrag = onTopDrag;
        return this;
    }

    @Override
    public PaginatedGui setOnBottomDrag(Consumer<InventoryDragEvent> onBottomDrag) {
        this.onBottomDrag = onBottomDrag;
        return this;
    }

    @Override
    public PaginatedGui setOnClose(Consumer<InventoryCloseEvent> onClose) {
        this.onClose = onClose;
        return this;
    }

    @Override
    public boolean cancelClick() {
        return cancelClick;
    }

    @Override
    @Nullable
    public GuiItem getItem(int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return null;
        }

        // Check static items first
        for (GuiItem item : staticItems) {
            if (item.getX() + item.getY() * 9 == slot) {
                return item;
            }
        }

        // If in bottom row, handle navigation buttons
        int bottomRow = (rows - 1) * 9;
        if (slot >= bottomRow) {
            return null;
        }

        List<GuiItem> items = pages.getOrDefault(currentPage, new ArrayList<>());

        if (usePattern) {
            // Find the index in the pattern
            for (int i = 0; i < Math.min(slotPattern.length, items.size()); i++) {
                if (slotPattern[i] == slot) {
                    return items.get(i);
                }
            }
        } else {
            // Convert slot to page item index
            int x = slot % 9;
            int y = slot / 9;
            int index = x + y * 9;

            if (index < items.size()) {
                return items.get(index);
            }
        }

        return null;
    }

    @Override
    public @NotNull List<GuiItem> getItems() {
        List<GuiItem> allItems = new ArrayList<>();

        // Add static items
        allItems.addAll(staticItems);

        // Add paginated items
        List<GuiItem> items = pages.getOrDefault(currentPage, new ArrayList<>());
        allItems.addAll(items);

        return allItems;
    }

    @Override
    public void onClose(Player player) {
        openers.remove(player);
        if (openers.isEmpty()) {
            GuiManager.markAsClosed(this);
        }
    }

    @Override
    public void updateItem(GuiItem item) {
        renderPage();
    }

    @Override
    public void updateItemAt(int slot) {
        renderPage();
    }

    @Override
    public GuiHolder closeAll() {
        for (Player player : openers) {
            close(player);
        }
        return this;
    }

    @Override
    public void autoUpdate() {
        GuiManager.autoUpdate(openers, autoUpdatePending, () -> {
            if (getItems().stream().anyMatch(item -> item.getState() == State.AUTO_UPDATE)) {
                refresh();
            }
        });
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getUID() {
        return UID;
    }
}
