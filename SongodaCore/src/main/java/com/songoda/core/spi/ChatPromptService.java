package com.songoda.core.spi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Paper chat prompt handling.
 */
public interface ChatPromptService {

    /**
     * The consumer is called on the player's owning entity scheduler for a response or on the player's quit event with {@code null}. If a queued response reaches the entity scheduler's retired callback, it is discarded because the player-owned callback can no longer run. The consumer is called at most once.
     *
     * @param player the player to prompt
     * @param consumer receives the chat response, or {@code null} if the player leaves before responding
     */
    void promptPlayer(@NotNull Player player, @NotNull Consumer<@Nullable String> consumer);
}
