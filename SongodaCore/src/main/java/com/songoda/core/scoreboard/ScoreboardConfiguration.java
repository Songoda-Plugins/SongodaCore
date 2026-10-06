package com.songoda.core.scoreboard;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parsed scoreboard configuration containing placeholder intervals, animations, and layouts.
 */
public final class ScoreboardConfiguration {

    private final Map<String, Long> placeholderUpdates;
    private final Map<String, ScoreboardAnimation> animations;
    private final Map<String, ScoreboardDefinition> scoreboards;

    public ScoreboardConfiguration(
            @NotNull Map<String, Long> placeholderUpdates,
            @NotNull Map<String, ScoreboardAnimation> animations,
            @NotNull Map<String, ScoreboardDefinition> scoreboards) {
        this.placeholderUpdates = Collections.unmodifiableMap(new LinkedHashMap<>(placeholderUpdates));
        this.animations = Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        this.scoreboards = Collections.unmodifiableMap(new LinkedHashMap<>(scoreboards));
    }

    /**
     * @return placeholder token to refresh interval in ticks
     */
    public @NotNull Map<String, Long> placeholderUpdates() {
        return placeholderUpdates;
    }

    /**
     * @return named animations
     */
    public @NotNull Map<String, ScoreboardAnimation> animations() {
        return animations;
    }

    /**
     * @return named scoreboard layouts
     */
    public @NotNull Map<String, ScoreboardDefinition> scoreboards() {
        return scoreboards;
    }
}
