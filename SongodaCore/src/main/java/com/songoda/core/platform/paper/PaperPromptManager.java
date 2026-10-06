package com.songoda.core.platform.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.vortexdevelopment.vinject.annotation.DependsOn;
import net.vortexdevelopment.vinject.annotation.lifecycle.OnDestroy;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.spi.ChatPromptService;
import com.songoda.core.spi.ChatPrompts;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@DependsOn(AsyncChatEvent.class)
@RegisterListener
public class PaperPromptManager implements Listener, ChatPromptService {

    private final Map<UUID, PendingPrompt> promptPlayers = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerChatPromptFacade() {
        ChatPrompts.setService(this);
    }

    @OnDestroy
    public void unregisterChatPromptFacade() {
        promptPlayers.clear();
        ChatPrompts.clearService(this);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PendingPrompt prompt = promptPlayers.get(playerId);
        if (prompt != null) {
            event.setCancelled(true);
            if (prompt.responseQueued.compareAndSet(false, true)) {
                Component shaded = AdventureUtils.convertToShadedComponent(event.originalMessage());
                String input = LegacyComponentSerializer.legacySection().serialize(shaded);
                SchedulerUtils.runEntityTask(SongodaPlugin.getInstance(), player, () -> {
                    completePrompt(playerId, prompt, input);
                }, () -> promptPlayers.remove(playerId, prompt));
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PendingPrompt prompt = promptPlayers.remove(player.getUniqueId());
        if (prompt != null) {
            prompt.consumer.accept(null);
        }
    }

    /**
     * Prompt a player for a response
     *
     * @param player   The player to prompt
     * @param consumer The consumer to handle the response. Response value can be null if the player leaves the server before responding
     */
    @Override
    public void promptPlayer(@NotNull Player player, @NotNull Consumer<@Nullable String> consumer) {
        promptPlayers.put(player.getUniqueId(), new PendingPrompt(consumer));
    }

    private void completePrompt(UUID playerId, PendingPrompt prompt, @Nullable String response) {
        if (promptPlayers.remove(playerId, prompt)) {
            prompt.consumer.accept(response);
        }
    }

    private static final class PendingPrompt {

        private final Consumer<@Nullable String> consumer;
        private final AtomicBoolean responseQueued = new AtomicBoolean();

        private PendingPrompt(Consumer<@Nullable String> consumer) {
            this.consumer = consumer;
        }
    }
}
