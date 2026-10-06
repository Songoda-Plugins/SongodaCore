package com.songoda.core.spi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Static entry for chat prompts; the implementation is registered by the Paper platform module.
 */
public final class ChatPrompts {

    private static volatile ChatPromptService service;

    private ChatPrompts() {
    }

    /**
     * Register the platform prompt service.
     *
     * @param service the service exposed to plugins
     */
    public static void setService(@NotNull ChatPromptService service) {
        ChatPrompts.service = service;
    }

    /**
     * Clear the service only if it is still the expected instance.
     *
     * @param expectedService the service being removed
     */
    public static void clearService(@NotNull ChatPromptService expectedService) {
        if (service == expectedService) {
            service = null;
        }
    }

    /**
     * Begin a chat prompt for a player.
     *
     * @param player the player to prompt
     * @param consumer receives the response on the player's entity scheduler, or {@code null} from the player's quit event
     * @throws IllegalStateException if the platform prompt service has not been initialized
     */
    public static void promptPlayer(@NotNull Player player, @NotNull Consumer<@Nullable String> consumer) {
        ChatPromptService s = service;
        if (s == null) {
            throw new IllegalStateException("ChatPromptService not initialized (wrong SongodaCore artifact or startup order)");
        }
        s.promptPlayer(player, consumer);
    }
}
