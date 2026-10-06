package com.songoda.core.scoreboard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedNumberFormat;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.songoda.core.text.AdventureUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A per-viewer packet scoreboard. It never creates a Bukkit scoreboard and does not schedule repeating tasks.
 */
public final class PacketScoreboard {

    /* Modern clients accept component team prefixes/suffixes; keeping each part at 128 gives a 256-character line. */
    private static final int MODERN_PART_LENGTH = 128;
    private static final int LEGACY_PART_LENGTH = 16;
    private static final AtomicInteger OBJECTIVE_COUNTER = new AtomicInteger();
    private static final AtomicLong REQUEST_COUNTER = new AtomicLong();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ScoreboardService service;
    private final String objectiveName;
    private final Map<UUID, ViewerState> viewers = new ConcurrentHashMap<>();

    private volatile Component title;
    private volatile boolean destroyed;

    PacketScoreboard(@NotNull ScoreboardService service, @NotNull Component title) {
        this.service = service;
        this.title = title;
        this.objectiveName = nextObjectiveName();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writeEntries(@NotNull PacketContainer packet, @NotNull List<String> entries) {
        if (packet.getStringArrays().size() > 0) {
            packet.getStringArrays().writeSafely(0, entries.toArray(new String[0]));
            return;
        }
        com.comphenix.protocol.reflect.StructureModifier modifier = packet.getModifier().withType(Collection.class);
        if (modifier.size() > 0) {
            modifier.writeSafely(0, entries);
        }
    }

    private static WrappedChatComponent chat(@NotNull Component component) {
        return WrappedChatComponent.fromJson(AdventureUtils.convertToJson(component));
    }

    private static List<String> splitLegacy(@NotNull String value, int maxVisibleCharacters) {
        if (value.isEmpty()) {
            return List.of("");
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String activeFormatting = "";
        int visibleCharacters = 0;
        for (int index = 0; index < value.length(); ) {
            char character = value.charAt(index);
            if (character == '§' && index + 1 < value.length()) {
                if (Character.toLowerCase(value.charAt(index + 1)) == 'x'
                        && index + 13 < value.length()
                        && isHexSequence(value, index + 2)) {
                    String token = value.substring(index, index + 14);
                    current.append(token);
                    activeFormatting = token;
                    index += 14;
                    continue;
                }
                char code = value.charAt(index + 1);
                String token = value.substring(index, index + 2);
                current.append(token);
                activeFormatting = updateFormatting(activeFormatting, token, code);
                index += 2;
                continue;
            }

            int codePoint = value.codePointAt(index);
            if (visibleCharacters >= maxVisibleCharacters) {
                parts.add(current.toString());
                current = new StringBuilder(activeFormatting);
                visibleCharacters = 0;
            }
            current.appendCodePoint(codePoint);
            visibleCharacters++;
            index += Character.charCount(codePoint);
        }
        parts.add(current.toString());
        return parts;
    }

    private static boolean isHexSequence(@NotNull String value, int start) {
        for (int offset = 0; offset < 12; offset += 2) {
            if (value.charAt(start + offset) != '§'
                    || Character.digit(value.charAt(start + offset + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static String updateFormatting(@NotNull String active, @NotNull String token, char code) {
        char normalized = Character.toLowerCase(code);
        if ("0123456789abcdef".indexOf(normalized) >= 0) {
            return token;
        }
        if (normalized == 'r') {
            return "";
        }
        return active + token;
    }

    private static String uniqueEntry(int index) {
        StringBuilder entry = new StringBuilder("§0");
        int value = index;
        while (value > 0) {
            entry.append("§").append(Integer.toHexString(value & 0xF));
            value >>>= 4;
        }
        return entry.toString();
    }

    private static List<Component> immutableLines(@NotNull List<Component> lines) {
        List<Component> copy = new ArrayList<>(lines.size());
        for (Component line : lines) {
            copy.add(line == null ? Component.empty() : line);
        }
        return Collections.unmodifiableList(copy);
    }

    private static String nextObjectiveName() {
        int value = OBJECTIVE_COUNTER.incrementAndGet();
        return "vtxsb" + Integer.toString(value, 36);
    }

    /**
     * Shows this scoreboard to a player using its current lines.
     *
     * @param player player to update
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> show(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        return service.submit(() -> {
            ViewerState state = viewers.computeIfAbsent(playerId, ignored -> new ViewerState(player, title));
            state.player = player;
            state.request = REQUEST_COUNTER.incrementAndGet();
            state.visible = true;
            render(state, state.lines);
            return null;
        });
    }

    /**
     * Replaces a player's lines. Packet construction and sending happen on the scoreboard executor.
     * The first line in the list is displayed at the top.
     *
     * @param player player to update
     * @param lines  lines to display
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> setLines(@NotNull Player player, @NotNull List<Component> lines) {
        UUID playerId = player.getUniqueId();
        List<Component> snapshot = immutableLines(lines);
        return service.submit(() -> {
            ViewerState state = viewers.computeIfAbsent(playerId, ignored -> new ViewerState(player, title));
            state.player = player;
            state.request = REQUEST_COUNTER.incrementAndGet();
            state.visible = true;
            state.lines = snapshot;
            render(state, snapshot);
            return null;
        });
    }

    /**
     * Replaces a player's lines from MiniMessage/legacy strings.
     *
     * @param player player to update
     * @param lines  lines to display
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> setLines(@NotNull Player player, @NotNull String... lines) {
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) {
            components.add(AdventureUtils.formatComponent(line));
        }
        return setLines(player, components);
    }

    /**
     * Resolves and applies lines off the server thread. A newer request for the same player supersedes an older one.
     * This is intended for PlaceholderAPI/database-backed scoreboard content.
     *
     * @param player   player to update
     * @param provider asynchronous line provider
     * @return completion stage for the complete resolve-and-send operation
     */
    public @NotNull CompletableFuture<Void> setLinesAsync(
            @NotNull Player player,
            @NotNull ScoreboardLineProvider provider) {
        UUID playerId = player.getUniqueId();
        long request = REQUEST_COUNTER.incrementAndGet();
        return CompletableFuture
                .supplyAsync(() -> provider.provide(player), service.executor())
                .thenCompose(CompletionStage::toCompletableFuture)
                .thenApplyAsync(PacketScoreboard::immutableLines, service.executor())
                .thenAcceptAsync(lines -> applyAsyncResult(player, playerId, request, lines), service.executor());
    }

    /**
     * Convenience overload for providers returning a completed or asynchronous list.
     *
     * @param player   player to update
     * @param provider asynchronous list provider
     * @return completion stage for the complete resolve-and-send operation
     */
    public @NotNull CompletableFuture<Void> setLinesAsync(
            @NotNull Player player,
            @NotNull Supplier<? extends CompletionStage<? extends List<Component>>> provider) {
        return setLinesAsync(player, ignored -> provider.get());
    }

    /**
     * Changes the title and refreshes all visible viewers.
     *
     * @param title new title
     * @return completion stage for the packet updates
     */
    public @NotNull CompletableFuture<Void> setTitle(@NotNull Component title) {
        return service.submit(() -> {
            this.title = title;
            viewers.values().stream().filter(state -> state.visible).forEach(state -> {
                state.title = title;
                render(state, state.lines);
            });
            return null;
        });
    }

    /**
     * Changes the title for one viewer without changing other viewers.
     *
     * @param player player receiving the title
     * @param title  new title
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> setTitle(
            @NotNull Player player,
            @NotNull Component title) {
        UUID playerId = player.getUniqueId();
        return service.submit(() -> {
            ViewerState state = viewers.computeIfAbsent(playerId, ignored -> new ViewerState(player, title));
            state.player = player;
            state.title = title;
            state.visible = true;
            render(state, state.lines);
            return null;
        });
    }

    /**
     * Hides the scoreboard from a player and forgets that viewer's lines.
     *
     * @param player player to hide it from
     * @return completion stage for the packet update
     */
    public @NotNull CompletableFuture<Void> hide(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        return service.submit(() -> {
            ViewerState state = viewers.remove(playerId);
            if (state != null) {
                remove(state);
            }
            return null;
        });
    }

    /**
     * Destroys this scoreboard and removes it from all current viewers.
     *
     * @return completion stage for the packet updates
     */
    public @NotNull CompletableFuture<Void> destroy() {
        destroyed = true;
        return service.submit(() -> {
            removeAllViewersInternal();
            service.forget(this);
            return null;
        });
    }

    void removeAllViewersInternal() {
        viewers.values().forEach(this::remove);
        viewers.clear();
    }

    private void applyAsyncResult(
            @NotNull Player player,
            @NotNull UUID playerId,
            long request,
            @NotNull List<Component> lines) {
        if (destroyed) {
            return;
        }
        ViewerState state = viewers.computeIfAbsent(playerId, ignored -> new ViewerState(player, title));
        if (state.request > request) {
            return;
        }
        state.player = player;
        state.request = request;
        state.visible = true;
        state.lines = lines;
        render(state, lines);
    }

    private void render(@NotNull ViewerState state, @NotNull List<Component> lines) {
        if (!state.player.isOnline()) {
            return;
        }

        for (RenderedLine oldLine : state.renderedLines) {
            send(createTeamPacket(oldLine.teamName, oldLine.entry, false, null), state.player);
        }

        send(createObjectivePacket(ObjectiveAction.REMOVE, null), state.player);
        send(createObjectivePacket(ObjectiveAction.CREATE, state.title), state.player);
        send(createDisplayPacket(), state.player);

        List<RenderedLine> rendered = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            RenderedLine line = renderLine(index, lines.get(index));
            rendered.add(line);
            send(createTeamPacket(line.teamName, line.entry, true, line), state.player);
            send(createScorePacket(line.entry, lines.size() - index), state.player);
        }
        state.renderedLines = rendered;
    }

    private void remove(@NotNull ViewerState state) {
        if (!state.player.isOnline()) {
            return;
        }
        send(createObjectivePacket(ObjectiveAction.REMOVE, null), state.player);
        for (RenderedLine line : state.renderedLines) {
            send(createTeamPacket(line.teamName, line.entry, false, null), state.player);
        }
    }

    private PacketContainer createObjectivePacket(
            @NotNull ObjectiveAction action,
            @Nullable Component objectiveTitle) {
        PacketContainer packet = service.protocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
        packet.getModifier().writeDefaults();
        packet.getStrings().writeSafely(0, objectiveName);
        packet.getIntegers().writeSafely(0, action.packetValue);
        if (action == ObjectiveAction.CREATE && objectiveTitle != null) {
            packet.getChatComponents().writeSafely(0, chat(objectiveTitle));
            packet.getRenderTypes().writeSafely(0, EnumWrappers.RenderType.INTEGER);
        }
        hideNumberFormat(packet);
        return packet;
    }

    private PacketContainer createDisplayPacket() {
        PacketContainer packet = service.protocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);
        packet.getModifier().writeDefaults();
        packet.getDisplaySlots().writeSafely(0, EnumWrappers.DisplaySlot.SIDEBAR);
        packet.getStrings().writeSafely(0, objectiveName);
        return packet;
    }

    private PacketContainer createScorePacket(@NotNull String entry, int score) {
        PacketContainer packet = service.protocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);
        packet.getModifier().writeDefaults();
        packet.getStrings().writeSafely(0, entry);
        packet.getStrings().writeSafely(1, objectiveName);
        packet.getIntegers().writeSafely(0, score);
        if (packet.getScoreboardActions().size() > 0) {
            packet.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.CHANGE);
        }
        hideNumberFormat(packet);
        return packet;
    }

    private PacketContainer createTeamPacket(
            @NotNull String teamName,
            @NotNull String entry,
            boolean add,
            @Nullable RenderedLine line) {
        PacketContainer packet = service.protocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getModifier().writeDefaults();
        packet.getStrings().writeSafely(0, teamName);
        packet.getIntegers().writeSafely(0, add ? 0 : 1);

        if (add && service.modernTeamParameters() && packet.getOptionalTeamParameters().size() > 0) {
            WrappedTeamParameters parameters = WrappedTeamParameters.newBuilder()
                    .displayName(chat(Component.empty()))
                    .prefix(chat(LEGACY.deserialize(line.prefix)))
                    .suffix(chat(LEGACY.deserialize(line.suffix)))
                    .nametagVisibility("never")
                    .collisionRule("never")
                    .color(EnumWrappers.ChatFormatting.RESET)
                    .options(0)
                    .build();
            packet.getOptionalTeamParameters().writeSafely(0, Optional.of(parameters));
            writeEntries(packet, List.of(entry));
            return packet;
        }

        if (add) {
            packet.getChatComponents().writeSafely(0, chat(Component.empty()));
            packet.getChatComponents().writeSafely(1, chat(LEGACY.deserialize(line.prefix)));
            packet.getChatComponents().writeSafely(2, chat(LEGACY.deserialize(line.suffix)));
            writeEntries(packet, List.of(entry));
        }
        return packet;
    }

    private void hideNumberFormat(@NotNull PacketContainer packet) {
        if (service.blankNumberFormat() && packet.getNumberFormats().size() > 0) {
            packet.getNumberFormats().writeSafely(0, WrappedNumberFormat.blank());
        }
    }

    private RenderedLine renderLine(int index, @NotNull Component line) {
        String entry = uniqueEntry(index);
        String team = "vtx_sb_" + Integer.toString(index, 36);
        int partLength = service.modernLineLength() ? MODERN_PART_LENGTH : LEGACY_PART_LENGTH;
        List<String> parts = splitLegacy(LEGACY.serialize(line), partLength);
        String prefix = parts.get(0);
        // A team entry has one prefix and one suffix. Anything beyond those two protocol fields
        // cannot be represented by a single sidebar line and is intentionally truncated.
        String suffix = parts.size() > 1 ? parts.get(1) : "";
        return new RenderedLine(team, entry, prefix, suffix);
    }

    private void send(@NotNull PacketContainer packet, @NotNull Player player) {
        try {
            service.protocolManager().sendServerPacket(player, packet);
        } catch (Exception exception) {
            service.plugin().getLogger().warning("Could not send scoreboard packet: " + exception.getMessage());
        }
    }

    private enum ObjectiveAction {
        CREATE(0),
        REMOVE(1);

        private final int packetValue;

        ObjectiveAction(int packetValue) {
            this.packetValue = packetValue;
        }
    }

    private static final class ViewerState {

        private Player player;
        private Component title;
        private List<Component> lines = List.of();
        private List<RenderedLine> renderedLines = List.of();
        private long request;
        private boolean visible;

        private ViewerState(@NotNull Player player, @NotNull Component title) {
            this.player = player;
            this.title = title;
        }
    }

    private record RenderedLine(String teamName, String entry, String prefix, String suffix) {
    }

}
