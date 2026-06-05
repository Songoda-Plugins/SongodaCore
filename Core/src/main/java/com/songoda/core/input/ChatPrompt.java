package com.songoda.core.input;

import com.songoda.core.compatibility.folia.SchedulerRunnable;
import com.songoda.core.compatibility.folia.SchedulerTask;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ChatPrompt implements Listener {
    private static final List<UUID> REGISTERED = new ArrayList<>();

    private final Plugin plugin;
    private final ChatConfirmHandler handler;
    private SchedulerTask task;
    private OnClose onClose = null;
    private OnCancel onCancel = null;
    private Listener listener;

    private ChatPrompt(Plugin plugin, Player player, ChatConfirmHandler handler) {
        this.plugin = plugin;
        this.handler = handler;

        REGISTERED.add(player.getUniqueId());
    }

    public static ChatPrompt showPrompt(Plugin plugin, Player player, ChatConfirmHandler handler) {
        return showPrompt(plugin, player, null, handler);
    }

    public static ChatPrompt showPrompt(Plugin plugin, Player player, String message, ChatConfirmHandler handler) {
        ChatPrompt prompt = new ChatPrompt(plugin, player, handler);
        prompt.startListener(plugin);
        player.closeInventory();

        if (message != null) {
            player.sendMessage(message);
        }

        return prompt;
    }

    public static boolean isRegistered(Player player) {
        return REGISTERED.contains(player.getUniqueId());
    }

    public static boolean unregister(Player player) {
        return REGISTERED.remove(player.getUniqueId());
    }

    public ChatPrompt setOnClose(OnClose onClose) {
        this.onClose = onClose;
        return this;
    }

    public ChatPrompt setOnCancel(OnCancel onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public ChatPrompt setTimeOut(Player player, long ticks) {
        task = SchedulerUtils.scheduleSyncDelayedTask(plugin, new SchedulerRunnable() {
            @Override
            public void run() {
                if (onClose != null) {
                    SchedulerUtils.scheduleSyncDelayedTask(plugin, new SchedulerRunnable() {
                        @Override
                        public void run() {
                            onClose.onClose();
                        }
                    }, 0L);
                }

                HandlerList.unregisterAll(listener);
                player.sendMessage("Your action has timed out.");
            }
        }, ticks);

        return this;
    }

    private void startListener(Plugin plugin) {
        this.listener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onChat(AsyncPlayerChatEvent event) {
                Player player = event.getPlayer();

                if (!ChatPrompt.isRegistered(player)) {
                    return;
                }

                ChatPrompt.unregister(player);
                event.setCancelled(true);

                ChatConfirmEvent chatConfirmEvent = new ChatConfirmEvent(player, event.getMessage());

                player.sendMessage("» " + event.getMessage());

                try {
                    ChatPrompt.this.handler.onChat(chatConfirmEvent);
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to process chat prompt", t);
                }

                if (onClose != null) {
                    SchedulerUtils.scheduleSyncDelayedTask(plugin, new SchedulerRunnable() {
                        @Override
                        public void run() {
                            onClose.onClose();
                        }
                    }, 0L);
                }

                HandlerList.unregisterAll(listener);
                SchedulerUtils.cancelTask(task);
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void onCancel(PlayerCommandPreprocessEvent event) {
                Player player = event.getPlayer();

                if (!ChatPrompt.isRegistered(player)) {
                    return;
                }

                ChatPrompt.unregister(player);

                if (event.getMessage().toLowerCase().startsWith("/cancel")) {
                    event.setCancelled(true);
                }

                if (onCancel != null) {
                    SchedulerUtils.scheduleSyncDelayedTask(plugin, new SchedulerRunnable() {
                        @Override
                        public void run() {
                            onCancel.onCancel();
                        }
                    }, 0L);
                } else if (onClose != null) {
                    SchedulerUtils.scheduleSyncDelayedTask(plugin, new SchedulerRunnable() {
                        @Override
                        public void run() {
                            onClose.onClose();
                        }
                    }, 0L);
                }

                HandlerList.unregisterAll(listener);
                SchedulerUtils.cancelTask(task);
            }
        };

        Bukkit.getPluginManager().registerEvents(this.listener, plugin);
    }

    public interface ChatConfirmHandler {
        void onChat(ChatConfirmEvent event);
    }

    public interface OnClose {
        void onClose();
    }

    public interface OnCancel {
        void onCancel();
    }

    public static class ChatConfirmEvent {
        private final Player player;
        private final String message;

        public ChatConfirmEvent(Player player, String message) {
            this.player = player;
            this.message = message;
        }

        public Player getPlayer() {
            return this.player;
        }

        public String getMessage() {
            return this.message;
        }
    }
}
