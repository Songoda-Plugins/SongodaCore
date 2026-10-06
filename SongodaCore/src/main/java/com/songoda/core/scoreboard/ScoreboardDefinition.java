package com.songoda.core.scoreboard;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A named scoreboard layout loaded from YAML.
 *
 * @param title       scoreboard title
 * @param lines       ordered lines, top to bottom
 * @param updateTicks default line refresh interval
 */
public record ScoreboardDefinition(
        @NotNull String title,
        @NotNull List<ScoreboardLineDefinition> lines,
        long updateTicks) {

    public ScoreboardDefinition {
        title = title == null ? "" : title;
        lines = List.copyOf(lines == null ? List.of() : lines);
        if (updateTicks < 0) {
            throw new IllegalArgumentException("Scoreboard update interval cannot be negative");
        }
    }
}
