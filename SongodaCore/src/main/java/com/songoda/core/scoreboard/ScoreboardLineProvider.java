package com.songoda.core.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Produces the lines for a packet scoreboard. Providers are invoked on the scoreboard executor.
 */
@FunctionalInterface
public interface ScoreboardLineProvider {

    /**
     * Resolves the lines for a player.
     *
     * @param player player receiving the scoreboard
     * @return a stage containing the lines, never {@code null}
     */
    @NotNull CompletionStage<? extends List<Component>> provide(@NotNull Player player);
}
