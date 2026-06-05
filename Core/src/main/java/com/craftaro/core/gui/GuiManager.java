package com.craftaro.core.gui;

import com.craftaro.core.compatibility.folia.SchedulerRunnable;
import com.craftaro.core.compatibility.folia.SchedulerUtils;
import com.craftaro.core.compatibility.ClientVersion;
import com.craftaro.core.compatibility.ServerVersion;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages events for GUI screens
 */
public class GuiManager {
    final Plugin plugin;
    final UUID uuid = UUID.randomUUID(); // manager tracking to fix weird bugs from lazy programming
    final GuiListener listener = new GuiListener(this);
    final Map<Player, Gui> openInventories = new HashMap<>();
    private final Object lock = new Object();
    private boolean initialized = false;
    private boolean shutdown = false;

    public GuiManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * Initialize the GUI handlers
     */
    public void init() {
        Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);

        this.initialized = true;
        this.shutdown = false;
    }

    /**
     * Check to see if this manager cannot open any more GUI screens
     *
     * @return true if the owning plugin has shutdown
     */
    public boolean isClosed() {
        return this.shutdown;
    }

    /**
     * Create and display a GUI interface for a player
     *
     * @param player player to open the interface for
     * @param gui    GUI to use
     */
    public void showGUI(Player player, Gui gui) {
        if (this.shutdown) {
            if (!this.plugin.isEnabled()) {
                return;
            }

            // recover if reloaded without calling init manually
            init();
        } else if (!this.initialized) {
            init();
        }

        if (gui instanceof AnvilGui) {
            // bukkit throws a fit now if you try to set anvil stuff asynchronously
            Gui openInv = this.openInventories.get(player);

            if (openInv != null) {
                openInv.open = false;
            }

            gui.getOrCreateInventory(this);
            ((AnvilGui) gui).open();
            gui.onOpen(this, player);

            synchronized (this.lock) {
                this.openInventories.put(player, gui);
            }

            return;
        }

        SchedulerUtils.runTaskAsynchronously(plugin, new SchedulerRunnable() {
            @Override
            public void run() {
                Gui openInv = openInventories.get(player);

                if (openInv != null) {
                    openInv.open = false;
                }

                Inventory inv = gui.getOrCreateInventory(GuiManager.this);

                SchedulerUtils.runTask(plugin, new SchedulerRunnable() {
                    @Override
                    public void run() {
                        player.openInventory(inv);
                        gui.onOpen(GuiManager.this, player);

                        synchronized (GuiManager.this.lock) {
                            GuiManager.this.openInventories.put(player, gui);
                        }
                    }
                });
            }
        });
    }

    public void showPopup(Player player, String message) {
        showPopup(player, message, XMaterial.NETHER_STAR, BackgroundType.ADVENTURE);
    }

    public void showPopup(Player player, String message, XMaterial icon) {
        showPopup(player, message, icon, BackgroundType.ADVENTURE);
    }

    public void showPopup(Player player, String message, XMaterial icon, BackgroundType background) {
        if (ClientVersion.getClientVersion(player).isAtLeast(ServerVersion.V1_12)) {
            PopupMessage popup = new PopupMessage(this.plugin, icon, message, background);
            popup.add();
            popup.grant(player);

            SchedulerUtils.runTaskLaterAsynchronously(plugin, new SchedulerRunnable() {
                @Override
                public void run() {
                    popup.revoke(player);
                    popup.remove();
                }
            }, 70);

            return;
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            player.sendTitle("", message, 10, 70, 10);
            return;
        }

        player.sendTitle("", message);
    }

    /**
     * Close all active GUIs
     */
    public void closeAll() {
        synchronized (this.lock) {
            this.openInventories.entrySet().stream()
                    .filter(e -> e.getKey().getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder)
                    .collect(Collectors.toList()) // to prevent concurrency exceptions
                    .forEach(e -> e.getKey().closeInventory());

            this.openInventories.clear();
        }
    }

    protected static class GuiListener implements Listener {
        final GuiManager manager;

        public GuiListener(GuiManager manager) {
            this.manager = manager;
        }

        @EventHandler(priority = EventPriority.LOW)
        void onDragGUI(InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Inventory openInv = event.getInventory();
            Gui gui;
            if (openInv.getHolder() != null && openInv.getHolder() instanceof GuiHolder
                    && ((GuiHolder) openInv.getHolder()).manager.uuid.equals(this.manager.uuid)) {
                gui = ((GuiHolder) openInv.getHolder()).getGUI();

                if (event.getRawSlots().stream()
                        .filter(slot -> gui.inventory.getSize() > slot)
                        .anyMatch(slot -> !gui.unlockedCells.getOrDefault(slot, false))) {
                    event.setCancelled(true);
                    event.setResult(Result.DENY);
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        void onClickGUI(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Inventory openInv = event.getInventory();
            final Player player = (Player) event.getWhoClicked();

            Gui gui;
            if (openInv.getHolder() != null && openInv.getHolder() instanceof GuiHolder &&
                    ((GuiHolder) openInv.getHolder()).manager.uuid.equals(this.manager.uuid)) {
                gui = ((GuiHolder) openInv.getHolder()).getGUI();

                if (event.getClick() == ClickType.DOUBLE_CLICK) {
                    // always cancel this event if there are matching gui elements, since it tends to do bad things
                    ItemStack clicked = event.getCursor();
                    if (clicked != null && clicked.getType() != Material.AIR) {
                        int cell = 0;
                        for (ItemStack it : gui.inventory.getContents()) {
                            if (!gui.unlockedCells.getOrDefault(cell++, false) && clicked.isSimilar(it)) {
                                event.setCancelled(true);

                                if (gui instanceof AnvilGui) {
                                    ((AnvilGui) gui).anvil.update();
                                }

                                break;
                            }
                        }
                    }
                }

                if (event.getSlotType() == SlotType.OUTSIDE) {
                    if (!gui.onClickOutside(this.manager, player, event)) {
                        event.setCancelled(true);
                    }
                } // did we click the gui or in the user's inventory?
                else if (event.getRawSlot() < gui.inventory.getSize()) { // or could use event.getClickedInventory() == gui.inventory
                    // allow event if this is not a GUI element
                    event.setCancelled(gui.unlockedCells.entrySet().stream().noneMatch(e -> event.getSlot() == e.getKey() && e.getValue()));

                    // process button press
                    if (gui.onClick(this.manager, player, openInv, event)) {
                        gui.getDefaultSound().play(player);
                    }
                } else {
                    // Player clicked in the bottom inventory while GUI is open
                    if (gui.onClickPlayerInventory(this.manager, player, openInv, event)) {
                        gui.getDefaultSound().play(player);
                    } else if (!gui.acceptsItems || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        event.setCancelled(true);

                        if (gui instanceof AnvilGui) {
                            ((AnvilGui) gui).anvil.update();
                        }
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        void onCloseGUI(InventoryCloseEvent event) {
            Inventory openInv = event.getInventory();

            if (openInv.getHolder() != null && openInv.getHolder() instanceof GuiHolder &&
                    ((GuiHolder) openInv.getHolder()).manager.uuid.equals(this.manager.uuid)) {
                Gui gui = ((GuiHolder) openInv.getHolder()).getGUI();

                if (gui instanceof AnvilGui) {
                    gui.inventory.clear();
                    gui.inventory = null;
                }

                if (!gui.open) {
                    return;
                }

                final Player player = (Player) event.getPlayer();
                if (!gui.allowDropItems) {
                    player.setItemOnCursor(null);
                }

                if (this.manager.shutdown) {
                    gui.onClose(this.manager, player);
                } else {
                    SchedulerUtils.runEntityTaskLater(manager.plugin, player, new SchedulerRunnable() {
                        @Override
                        public void run() {
                            gui.onClose(manager, player);
                        }
                    }, 1);
                }

                this.manager.openInventories.remove(player);
            }
        }

        @EventHandler
        void onDisable(PluginDisableEvent event) {
            if (event.getPlugin() == this.manager.plugin) {
                // uh-oh! Abandon ship!!
                this.manager.shutdown = true;
                this.manager.closeAll();
                this.manager.initialized = false;
            }
        }
    }
}
