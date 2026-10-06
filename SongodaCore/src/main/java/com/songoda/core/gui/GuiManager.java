package com.songoda.core.gui;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuiManager {

    private static final GuiListener inventoryListener = new GuiListener();
    private static final Set<GuiHolder> openGuis = ConcurrentHashMap.newKeySet();

    public static void markAsOpen(GuiHolder gui) {
        openGuis.add(gui);
    }

    public static void markAsClosed(GuiHolder gui) {
        openGuis.remove(gui);
    }

    public static void disable() {
        // Snapshot so closeAll / events cannot mutate the set we iterate; also used to find stragglers.
        Set<GuiHolder> snapshot = Set.copyOf(openGuis);
        Plugin plugin = SongodaPlugin.getInstance();
        boolean pluginEnabled = plugin.isEnabled();
        boolean canCloseSynchronously = !SchedulerUtils.isFolia() && Bukkit.isPrimaryThread();

        if (pluginEnabled || canCloseSynchronously) {
            for (GuiHolder gui : snapshot) {
                gui.closeAll();
            }

            // If openers ever desynced from the real client view, close by top-inventory holder.
            // Reflection: InventoryView was a class on older servers and is an interface on newer API;
            // compiling against either breaks the other with IncompatibleClassChangeError.
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (pluginEnabled) {
                    SchedulerUtils.runEntityTask(plugin, player, () -> closeTrackedInventory(player, snapshot));
                } else {
                    closeTrackedInventory(player, snapshot);
                }
            }
        }
        openGuis.clear();
    }

    private static void closeTrackedInventory(Player player, Set<GuiHolder> snapshot) {
        if (!player.isOnline()) {
            return;
        }

        Inventory top = openTopInventory(player);
        if (top == null) {
            return;
        }

        InventoryHolder holder = top.getHolder();
        if (holder instanceof GuiHolder guiHolder && snapshot.contains(guiHolder)) {
            player.closeInventory();
        }
    }

    public static void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(inventoryListener, plugin);
        // This timer coordinates updates; each GUI dispatches its work to a viewer's owner.
        SchedulerUtils.runTaskTimer(plugin, () -> {
            for (GuiHolder gui : openGuis) {
                gui.autoUpdate();
            }
        }, 0L, 20L);
    }

    static void autoUpdate(List<Player> openers, AtomicBoolean pending, Runnable update) {
        Plugin plugin = SongodaPlugin.getInstance();
        List<Player> viewers = List.copyOf(openers);
        if (!plugin.isEnabled() || viewers.isEmpty() || !pending.compareAndSet(false, true)) {
            return;
        }

        Player owner = viewers.get(0);
        Runnable release = () -> pending.set(false);
        try {
            SchedulerUtils.runEntityTask(plugin, owner, () -> {
                try {
                    if (plugin.isEnabled() && owner.isOnline() && openers.contains(owner)) {
                        update.run();
                    }
                } finally {
                    release.run();
                }
            }, release);
        } catch (RuntimeException e) {
            release.run();
            throw e;
        }
    }

    public static void unregister(Plugin plugin) {
        disable();
        HandlerList.unregisterAll(inventoryListener);
    }

    /**
     * For compatibility with older servers, we need to use reflection to get the top inventory because they changed the inventory view class to an interface.
     *
     * @param player The player to get the top inventory of.
     * @return The top inventory of the player, or null if the player is not open.
     */
    private static Inventory openTopInventory(Player player) {
        try {
            Method getOpen = Player.class.getMethod("getOpenInventory");
            Object view = getOpen.invoke(player);
            if (view == null) {
                return null;
            }
            Method getTop = view.getClass().getMethod("getTopInventory");
            Object top = getTop.invoke(view);
            return top instanceof Inventory inv ? inv : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
