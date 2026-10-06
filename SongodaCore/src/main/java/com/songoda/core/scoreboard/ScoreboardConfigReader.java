package com.songoda.core.scoreboard;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads scoreboard layouts from Bukkit or VInject configuration sections.
 */
public final class ScoreboardConfigReader {

    private ScoreboardConfigReader() {
    }

    /**
     * Reads the complete configuration from a Bukkit YAML section or root.
     *
     * @param section root or {@code Scoreboards} section
     * @return parsed configuration
     */
    public static @NotNull ScoreboardConfiguration read(@NotNull ConfigurationSection section) {
        return read(section.getValues(false));
    }

    /**
     * Reads the complete configuration from a VInject YAML section or root.
     *
     * @param section root or {@code Scoreboards} section
     * @return parsed configuration
     */
    public static @NotNull ScoreboardConfiguration read(
            @NotNull net.vortexdevelopment.vinject.config.ConfigurationSection section) {
        return read(section.getValues(false));
    }

    /**
     * Reads the complete configuration from a map, which is useful inside custom YAML serializers.
     *
     * @param values root or {@code Scoreboards} values
     * @return parsed configuration
     */
    public static @NotNull ScoreboardConfiguration read(@NotNull Map<String, Object> values) {
        Map<String, Object> root = normalizeMap(values);
        boolean isRoot = root.containsKey("Scoreboards")
                || root.containsKey("Animations")
                || root.containsKey("Placeholder Updates");
        Map<String, Object> section = isRoot ? root : Map.of();
        Map<String, Object> scoreboardValues = isRoot ? map(root.get("Scoreboards")) : root;

        Map<String, Long> placeholderUpdates = readPlaceholderUpdates(section.get("Placeholder Updates"));
        Map<String, ScoreboardAnimation> animations = readAnimations(map(section.get("Animations")));
        Map<String, ScoreboardDefinition> scoreboards = readScoreboards(scoreboardValues);
        return new ScoreboardConfiguration(placeholderUpdates, animations, scoreboards);
    }

    private static Map<String, Long> readPlaceholderUpdates(Object raw) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Long interval = number(entry.getValue());
                if (interval != null) {
                    result.put(normalizePlaceholder(String.valueOf(entry.getKey())), Math.max(0L, interval));
                }
            }
        } else if (raw instanceof List<?> list) {
            for (Object value : list) {
                String declaration = String.valueOf(value);
                int separator = declaration.lastIndexOf(':');
                if (separator <= 0 || separator == declaration.length() - 1) {
                    continue;
                }
                try {
                    result.put(normalizePlaceholder(declaration.substring(0, separator).trim()),
                            Math.max(0L, Long.parseLong(declaration.substring(separator + 1).trim())));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed optional placeholder declarations.
                }
            }
        }
        return result;
    }

    private static Map<String, ScoreboardAnimation> readAnimations(Map<String, Object> values) {
        Map<String, ScoreboardAnimation> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Map<String, Object> animation = map(entry.getValue());
            List<String> frames = strings(animation.get("Frames"));
            if (frames.isEmpty()) {
                frames = strings(animation.get("Lines"));
            }
            if (frames.isEmpty()) {
                continue;
            }
            long interval = firstPositive(animation, "Update", "Interval", "Update Ticks", 1L);
            result.put(entry.getKey(), new ScoreboardAnimation(frames, interval));
        }
        return result;
    }

    private static Map<String, ScoreboardDefinition> readScoreboards(Map<String, Object> values) {
        Map<String, ScoreboardDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Map<String, Object> scoreboard = map(entry.getValue());
            String title = string(scoreboard.get("Title"), "");
            long update = firstNonNegative(scoreboard, "Update", "Update Ticks", 0L);
            List<ScoreboardLineDefinition> lines = readLines(scoreboard.get("Lines"), update);
            result.put(entry.getKey(), new ScoreboardDefinition(title, lines, update));
        }
        return result;
    }

    private static List<ScoreboardLineDefinition> readLines(Object raw, long defaultUpdate) {
        List<ScoreboardLineDefinition> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                result.add(readLine(value, defaultUpdate));
            }
        } else if (raw instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                result.add(readLine(value, defaultUpdate));
            }
        }
        return result;
    }

    private static ScoreboardLineDefinition readLine(Object raw, long defaultUpdate) {
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return new ScoreboardLineDefinition(string(raw, ""), List.of(), null, defaultUpdate);
        }
        Map<String, Object> line = normalizeMap(rawMap);
        String text = firstString(line, "Text", "Line");
        String animation = stringOrNull(line.get("Animation"));
        List<String> frames = strings(line.get("Frames"));
        if (frames.isEmpty()) {
            frames = strings(line.get("Lines"));
        }
        long update = firstNonNegative(line, "Update", "Update Ticks", defaultUpdate);
        return new ScoreboardLineDefinition(text, frames, animation, update);
    }

    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? normalizeMap(map) : Collections.emptyMap();
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            result.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return result;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof ConfigurationSection section) {
            return normalizeMap(section.getValues(false));
        }
        if (value instanceof net.vortexdevelopment.vinject.config.ConfigurationSection section) {
            return normalizeMap(section.getValues(false));
        }
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object entry : list) {
                normalized.add(normalizeValue(entry));
            }
            return normalized;
        }
        return value;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry != null) {
                result.add(String.valueOf(entry));
            }
        }
        return result;
    }

    private static String firstString(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            String value = stringOrNull(values.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String string(Object value, String fallback) {
        String result = stringOrNull(value);
        return result == null ? fallback : result;
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static long firstPositive(Map<String, Object> values, String first, String second, String third, long fallback) {
        long value = firstNonNegative(values, first, second, third, fallback);
        return value > 0 ? value : fallback;
    }

    private static long firstNonNegative(Map<String, Object> values, String first, String second, long fallback) {
        for (String key : new String[]{first, second}) {
            Long value = number(values.get(key));
            if (value != null) {
                return Math.max(0L, value);
            }
        }
        return fallback;
    }

    private static long firstNonNegative(Map<String, Object> values, String first, String second, String third, long fallback) {
        for (String key : new String[]{first, second, third}) {
            Long value = number(values.get(key));
            if (value != null) {
                return Math.max(0L, value);
            }
        }
        return fallback;
    }

    private static String normalizePlaceholder(String placeholder) {
        String value = placeholder.trim();
        if (!value.startsWith("%")) {
            value = "%" + value;
        }
        if (!value.endsWith("%")) {
            value += "%";
        }
        return value;
    }
}
