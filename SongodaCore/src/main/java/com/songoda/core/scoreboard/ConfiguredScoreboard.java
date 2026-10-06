package com.songoda.core.scoreboard;

import net.kyori.adventure.text.Component;
import com.songoda.core.text.AdventureUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime scoreboard created from a {@link ScoreboardDefinition}. It manages per-viewer refreshes and animation frames.
 */
public final class ConfiguredScoreboard {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%\\s]+%");

    private final ScoreboardService service;
    private final ScoreboardConfiguration configuration;
    private final ScoreboardDefinition definition;
    private final PacketScoreboard scoreboard;
    private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();
    private final boolean placeholderApiAvailable;

    private volatile boolean destroyed;

    ConfiguredScoreboard(
            @NotNull ScoreboardService service,
            @NotNull ScoreboardConfiguration configuration,
            @NotNull ScoreboardDefinition definition) {
        this.service = service;
        this.configuration = configuration;
        this.definition = definition;
        // The title may contain player-specific placeholders, so it is resolved on the first viewer render.
        this.scoreboard = service.create(Component.empty());
        this.placeholderApiAvailable = service.placeholderApiAvailable();
    }

    private static long minPositive(long current, long candidate) {
        if (candidate <= 0) {
            return current;
        }
        return current <= 0 ? candidate : Math.min(current, candidate);
    }

    private static String frame(@NotNull List<String> frames, long intervalTicks, long elapsedTicks) {
        int index = (int) ((elapsedTicks / Math.max(1L, intervalTicks)) % frames.size());
        return frames.get(index);
    }

    private static String animationToken(@NotNull String text) {
        if (text.startsWith("<animation:") && text.endsWith(">")) {
            return text.substring("<animation:".length(), text.length() - 1);
        }
        if (text.startsWith("<") && text.endsWith(">") && text.indexOf(' ', 1) < 0) {
            return text.substring(1, text.length() - 1);
        }
        return null;
    }

    /**
     * Shows the configured scoreboard and starts only the refresh task required by its contents.
     *
     * @param player viewer
     * @return completion stage for the initial packet update
     */
    public @NotNull CompletableFuture<Void> show(@NotNull Player player) {
        Viewer previous = viewers.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        Viewer viewer = new Viewer(player, definition.lines().size());
        viewers.put(player.getUniqueId(), viewer);
        CompletableFuture<Void> initial = scoreboard.show(player);
        long refreshTicks = refreshIntervalTicks();
        if (refreshTicks > 0) {
            viewer.future = service.scheduler().scheduleWithFixedDelay(
                    () -> refresh(viewer), 0L, refreshTicks * 50L, TimeUnit.MILLISECONDS);
        } else {
            service.scheduler().execute(() -> refresh(viewer));
        }
        return initial;
    }

    /**
     * Hides the configured scoreboard from a player and stops that viewer's refresh task.
     *
     * @param player viewer
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> hide(@NotNull Player player) {
        Viewer viewer = viewers.remove(player.getUniqueId());
        if (viewer != null) {
            viewer.cancel();
        }
        return scoreboard.hide(player);
    }

    /**
     * Destroys the configured scoreboard and stops all refresh tasks.
     *
     * @return completion stage for cleanup
     */
    public @NotNull CompletableFuture<Void> destroy() {
        destroyed = true;
        viewers.values().forEach(Viewer::cancel);
        viewers.clear();
        return scoreboard.destroy();
    }

    /**
     * @return the underlying packet scoreboard for manual updates or title changes
     */
    public @NotNull PacketScoreboard packetScoreboard() {
        return scoreboard;
    }

    private void refresh(@NotNull Viewer viewer) {
        if (destroyed || viewers.get(viewer.player.getUniqueId()) != viewer) {
            viewer.cancel();
            return;
        }

        long elapsedTicks = Math.max(0L,
                (System.nanoTime() - viewer.startedAtNanos) / 50_000_000L);
        boolean titleChanged = false;
        if (elapsedTicks >= viewer.nextTitleTick) {
            Component title = resolveComponent(viewer, definition.title(), elapsedTicks, true);
            titleChanged = !title.equals(viewer.title);
            viewer.title = title;
            viewer.nextTitleTick = titleIntervalTicks() > 0
                    ? elapsedTicks + titleIntervalTicks()
                    : Long.MAX_VALUE;
        }

        List<Component> lines = new ArrayList<>(definition.lines().size());
        boolean linesChanged = false;
        for (int index = 0; index < definition.lines().size(); index++) {
            ScoreboardLineDefinition line = definition.lines().get(index);
            long interval = lineIntervalTicks(line);
            if (elapsedTicks >= viewer.nextLineTicks[index]) {
                Component rendered = renderLine(viewer, line, elapsedTicks);
                linesChanged |= !rendered.equals(viewer.lines.get(index));
                viewer.lines.set(index, rendered);
                viewer.nextLineTicks[index] = interval > 0
                        ? elapsedTicks + interval
                        : Long.MAX_VALUE;
            }
            lines.add(viewer.lines.get(index));
        }

        if (titleChanged) {
            scoreboard.setTitle(viewer.player, viewer.title);
        }
        if (linesChanged || viewer.firstRender) {
            viewer.firstRender = false;
            scoreboard.setLines(viewer.player, lines);
        }
    }

    private Component renderLine(
            @NotNull Viewer viewer,
            @NotNull ScoreboardLineDefinition line,
            long elapsedTicks) {
        String text = line.text();
        List<String> frames = line.frames();
        if (line.animation() != null) {
            ScoreboardAnimation animation = configuration.animations().get(line.animation());
            if (animation != null) {
                text = frame(animation.frames(), animation.updateTicks(), elapsedTicks);
            }
        } else if (!frames.isEmpty()) {
            text = frame(frames, Math.max(1L, line.updateTicks()), elapsedTicks);
        } else if (text != null) {
            String animationName = animationToken(text);
            if (animationName != null) {
                ScoreboardAnimation animation = configuration.animations().get(animationName);
                if (animation != null) {
                    text = frame(animation.frames(), animation.updateTicks(), elapsedTicks);
                }
            }
        }
        return resolveComponent(viewer, text == null ? "" : text, elapsedTicks, true);
    }

    private Component resolveComponent(
            @NotNull Viewer viewer,
            @NotNull String text,
            long elapsedTicks,
            boolean lineRefresh) {
        String resolved = resolvePlaceholders(viewer, text, elapsedTicks, lineRefresh);
        return AdventureUtils.formatComponent(resolved);
    }

    private String resolvePlaceholders(
            @NotNull Viewer viewer,
            @NotNull String text,
            long elapsedTicks,
            boolean lineRefresh) {
        if (!placeholderApiAvailable || !text.contains("%")) {
            return text;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group();
            CachedPlaceholder cached = viewer.placeholders.get(placeholder);
            long updateTicks = placeholderIntervalTicks(placeholder);
            boolean explicitlyConfigured = configuration.placeholderUpdates().containsKey(placeholder)
                    || configuration.placeholderUpdates().containsKey(placeholder.toLowerCase());
            boolean shouldResolve = cached == null
                    || (!explicitlyConfigured && lineRefresh)
                    || (updateTicks > 0 && elapsedTicks >= cached.updatedAtTick + updateTicks);
            if (shouldResolve) {
                String value = PlaceholderApiResolver.resolve(viewer.player, placeholder);
                cached = new CachedPlaceholder(value, elapsedTicks);
                viewer.placeholders.put(placeholder, cached);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(cached.value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private long refreshIntervalTicks() {
        long interval = titleIntervalTicks();
        for (ScoreboardLineDefinition line : definition.lines()) {
            long lineInterval = lineIntervalTicks(line);
            if (lineInterval > 0 && (interval == 0 || lineInterval < interval)) {
                interval = lineInterval;
            }
        }
        return interval;
    }

    private long titleIntervalTicks() {
        return placeholderIntervalForText(definition.title());
    }

    private long lineIntervalTicks(@NotNull ScoreboardLineDefinition line) {
        long interval = line.updateTicks() > 0 ? line.updateTicks() : definition.updateTicks();
        if (line.animation() != null) {
            ScoreboardAnimation animation = configuration.animations().get(line.animation());
            if (animation != null) {
                interval = minPositive(interval, animation.updateTicks());
            }
        } else if (!line.frames().isEmpty()) {
            interval = minPositive(interval, Math.max(1L, line.updateTicks()));
        } else if (line.text() != null) {
            String animationName = animationToken(line.text());
            ScoreboardAnimation animation = configuration.animations().get(animationName);
            if (animation != null) {
                interval = minPositive(interval, animation.updateTicks());
                for (String frame : animation.frames()) {
                    interval = minPositive(interval, placeholderIntervalForText(frame));
                }
            }
        }
        long placeholderInterval = placeholderIntervalForText(line.text());
        if (!line.frames().isEmpty()) {
            for (String frame : line.frames()) {
                placeholderInterval = minPositive(placeholderInterval, placeholderIntervalForText(frame));
            }
        }
        if (line.animation() != null) {
            ScoreboardAnimation animation = configuration.animations().get(line.animation());
            if (animation != null) {
                for (String frame : animation.frames()) {
                    placeholderInterval = minPositive(placeholderInterval, placeholderIntervalForText(frame));
                }
            }
        }
        return minPositive(interval, placeholderInterval);
    }

    private long placeholderIntervalForText(String text) {
        if (!placeholderApiAvailable || text == null) {
            return 0L;
        }
        long interval = 0L;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            interval = minPositive(interval, placeholderIntervalTicks(matcher.group()));
        }
        return interval;
    }

    private long placeholderIntervalTicks(@NotNull String placeholder) {
        Long value = configuration.placeholderUpdates().get(placeholder);
        if (value == null) {
            value = configuration.placeholderUpdates().get(placeholder.toLowerCase());
        }
        return value == null ? 0L : Math.max(0L, value);
    }

    private static final class Viewer {

        private final Player player;
        private final long startedAtNanos = System.nanoTime();
        private final List<Component> lines;
        private final long[] nextLineTicks;
        private final Map<String, CachedPlaceholder> placeholders = new ConcurrentHashMap<>();
        private Component title = Component.empty();
        private long nextTitleTick;
        private boolean firstRender = true;
        private ScheduledFuture<?> future;

        private Viewer(@NotNull Player player, int lineCount) {
            this.player = player;
            this.lines = new ArrayList<>(Collections.nCopies(lineCount, Component.empty()));
            this.nextLineTicks = new long[lineCount];
        }

        private void cancel() {
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    private record CachedPlaceholder(String value, long updatedAtTick) {
    }
}
