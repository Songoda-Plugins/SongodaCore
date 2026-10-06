package com.songoda.core.scoreboard;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.WrappedNumberFormat;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import net.kyori.adventure.text.Component;
import com.songoda.core.SongodaCore;
import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.text.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opt-in manager for asynchronous, ProtocolLib-backed packet scoreboards.
 *
 * <p>No executor or ProtocolLib manager is created until {@link #init()} is called. A plugin should call
 * {@code ScoreboardService.init()} from its enable callback when it needs packet scoreboards.</p>
 */
public final class ScoreboardService {

    private static volatile ScoreboardService instance;

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final ScheduledExecutorService executor;
    private final Set<PacketScoreboard> scoreboards = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final boolean modernLineLength;
    private final boolean blankNumberFormat;
    private final boolean teamParametersSupported;
    private final boolean placeholderApiAvailable;

    private volatile boolean closed;

    private ScoreboardService(@NotNull Plugin plugin, @NotNull ProtocolManager protocolManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.executor = Executors.newSingleThreadScheduledExecutor(new ScoreboardThreadFactory(plugin));
        this.modernLineLength = ServerVersion.isAtLeastVersion(KnownServerVersions.V1_13);
        this.blankNumberFormat = ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_3)
                && WrappedNumberFormat.isSupported();
        this.teamParametersSupported = WrappedTeamParameters.isSupported();
        this.placeholderApiAvailable = PlaceholderApiResolver.isAvailable();
    }

    /**
     * Initializes the service for the current Songoda plugin.
     *
     * @return the initialized service
     * @throws IllegalStateException if SongodaCore has no active plugin or ProtocolLib is not enabled
     */
    public static @NotNull ScoreboardService init() {
        ScoreboardService current = instance;
        if (current != null && !current.closed) {
            return current;
        }

        synchronized (ScoreboardService.class) {
            current = instance;
            if (current != null && !current.closed) {
                return current;
            }

            Plugin plugin = SongodaCore.getPlugin();
            if (plugin == null) {
                throw new IllegalStateException("ScoreboardService.init() must be called after the plugin is enabled");
            }
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null
                    || !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                throw new IllegalStateException("ScoreboardService requires an enabled ProtocolLib installation");
            }

            current = new ScoreboardService(plugin, ProtocolLibrary.getProtocolManager());
            instance = current;
            return current;
        }
    }

    /**
     * Returns the initialized service without creating it.
     *
     * @return the service, or {@code null} when {@link #init()} has not been called
     */
    public static @Nullable ScoreboardService get() {
        ScoreboardService current = instance;
        return current == null || current.closed ? null : current;
    }

    /**
     * Shuts down the service and removes all scoreboards created by it.
     * This is called automatically by {@code SongodaPlugin} during plugin shutdown.
     */
    public static void shutdown() {
        ScoreboardService current;
        synchronized (ScoreboardService.class) {
            current = instance;
            instance = null;
        }
        if (current != null) {
            current.close();
        }
    }

    /**
     * Creates an empty packet scoreboard.
     *
     * @param title scoreboard title
     * @return a new scoreboard
     */
    public @NotNull PacketScoreboard create(@NotNull Component title) {
        ensureOpen();
        PacketScoreboard scoreboard = new PacketScoreboard(this, title);
        scoreboards.add(scoreboard);
        return scoreboard;
    }

    /**
     * Creates an empty packet scoreboard from a MiniMessage/legacy title.
     *
     * @param title scoreboard title
     * @return a new scoreboard
     */
    public @NotNull PacketScoreboard create(@NotNull String title) {
        return create(AdventureUtils.formatComponent(title));
    }

    /**
     * Creates a scoreboard whose title, lines, animations, and refresh intervals come from a parsed configuration.
     *
     * @param configuration parsed scoreboard configuration
     * @param name          configured scoreboard name
     * @return configured scoreboard
     * @throws IllegalArgumentException if the configured name does not exist
     */
    public @NotNull ConfiguredScoreboard create(
            @NotNull ScoreboardConfiguration configuration,
            @NotNull String name) {
        ScoreboardDefinition definition = configuration.scoreboards().get(name);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown scoreboard: " + name);
        }
        return new ConfiguredScoreboard(this, configuration, definition);
    }

    Plugin plugin() {
        return plugin;
    }

    ProtocolManager protocolManager() {
        return protocolManager;
    }

    boolean modernLineLength() {
        return modernLineLength;
    }

    boolean blankNumberFormat() {
        return blankNumberFormat;
    }

    boolean modernTeamParameters() {
        return teamParametersSupported;
    }

    ExecutorService executor() {
        return executor;
    }

    ScheduledExecutorService scheduler() {
        return executor;
    }

    boolean placeholderApiAvailable() {
        return placeholderApiAvailable;
    }

    void forget(@NotNull PacketScoreboard scoreboard) {
        scoreboards.remove(scoreboard);
    }

    <T> CompletableFuture<T> submit(java.util.function.Supplier<T> action) {
        ensureOpen();
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                result.complete(action.get());
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });
        return result;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ScoreboardService is shut down");
        }
    }

    private void close() {
        closed = true;
        try {
            executor.submit(() -> scoreboards.forEach(PacketScoreboard::removeAllViewersInternal)).get();
        } catch (Exception ignored) {
            // The server may already be stopping; packet cleanup is best effort in that case.
        } finally {
            scoreboards.clear();
            executor.shutdownNow();
        }
    }

    private static final class ScoreboardThreadFactory implements ThreadFactory {

        private final String threadName;
        private final AtomicInteger counter = new AtomicInteger();

        private ScoreboardThreadFactory(@NotNull Plugin plugin) {
            this.threadName = plugin.getName() + "-scoreboard";
        }

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, threadName + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
