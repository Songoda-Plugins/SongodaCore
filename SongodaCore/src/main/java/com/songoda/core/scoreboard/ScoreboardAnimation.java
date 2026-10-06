package com.songoda.core.scoreboard;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A named scoreboard animation.
 *
 * @param frames      animation frames, displayed in order
 * @param updateTicks number of server-tick durations between frames
 */
public record ScoreboardAnimation(@NotNull List<String> frames, long updateTicks) {

    public ScoreboardAnimation {
        frames = List.copyOf(frames);
        if (updateTicks < 1) {
            throw new IllegalArgumentException("Animation update interval must be at least one tick");
        }
    }
}
