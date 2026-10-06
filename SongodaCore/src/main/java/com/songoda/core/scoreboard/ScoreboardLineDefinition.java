package com.songoda.core.scoreboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Configuration for one displayed scoreboard line.
 *
 * @param text        line text, optionally containing an animation token
 * @param frames      inline animation frames for legacy configuration layouts
 * @param animation   named animation to play
 * @param updateTicks line refresh interval, or zero for initial render only
 */
public record ScoreboardLineDefinition(
        @Nullable String text,
        @NotNull List<String> frames,
        @Nullable String animation,
        long updateTicks) {

    public ScoreboardLineDefinition {
        frames = frames == null ? List.of() : List.copyOf(frames);
        if (updateTicks < 0) {
            throw new IllegalArgumentException("Line update interval cannot be negative");
        }
    }

    /**
     * Creates a static line.
     *
     * @param text line text
     * @return line definition
     */
    public static ScoreboardLineDefinition text(@Nullable String text) {
        return new ScoreboardLineDefinition(text, List.of(), null, 0);
    }

    /**
     * Creates a named animation line.
     *
     * @param animation animation key
     * @return line definition
     */
    public static ScoreboardLineDefinition animation(@Nullable String animation) {
        return new ScoreboardLineDefinition(null, List.of(), animation, 0);
    }
}
