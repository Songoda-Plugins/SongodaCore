package com.songoda.core.text.hologram;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.text.MiniMessagePlaceholder;
import com.songoda.core.text.lang.Lang;
import com.songoda.core.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Text holograms. A hologram can be rendered either with Bukkit entities or with
 * per-player ProtocolLib packets, depending on {@link HologramManager}.
 *
 * <p>Calculators passed to {@link #updateAsync(Supplier)} and providers registered
 * with {@link #registerAsyncPlaceholder(HologramPlaceholderProvider, long)} must
 * only use thread-safe data. They are deliberately executed away from the server
 * tick thread.</p>
 */
public class Hologram {

    @Getter
    private final String id;
    private final Location location;
    private final UUID worldId;
    private final Object stateLock = new Object();
    private final List<UUID> viewers = new CopyOnWriteArrayList<>();
    private final Map<String, HologramPlaceholder> placeholders = new HashMap<>();

    private List<String> lines = List.of();
    private volatile boolean useViewers;
    @Getter
    private volatile boolean visible = true;
    private volatile boolean shouldUpdate = true;
    private long asyncUpdateSequence;
    private long placeholderRevision;

    /**
     * Packet-backend render cache. Guarded by {@link #stateLock}.
     */
    private HologramPacketSnapshot cachedPacketSnapshot;
    private List<String> cachedPacketLines = List.of();
    private List<Component> cachedPacketComponents = List.of();
    private long cachedPlaceholderRevision = -1L;
    private long cachedStaticPlaceholdersRevision = -1L;

    /**
     * Kept for the Bukkit backend and source compatibility. Packet holograms do
     * not populate this list because they have no Bukkit entity instances.
     */
    @Getter
    @Setter
    private List<ArmorStand> armorStands = new CopyOnWriteArrayList<>();

    public Hologram(String id, Location location) {
        this.id = id;
        this.location = requireLocation(location);
        this.worldId = this.location.getWorld().getUID();
    }

    public Hologram(String id, Location location, String... lines) {
        this(id, location);
        setLines(List.of(lines));
    }

    public Hologram(String id, Location location, List<String> lines) {
        this(id, location);
        setLines(lines);
    }

    private static Location requireLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Hologram location and world cannot be null");
        }
        return location.clone();
    }

    public Location getLocation() {
        return location.clone();
    }

    UUID worldId() {
        return worldId;
    }

    int chunkX() {
        return location.getBlockX() >> 4;
    }

    int chunkZ() {
        return location.getBlockZ() >> 4;
    }

    public List<UUID> getViewers() {
        return viewers;
    }

    public void setUseViewers(boolean useViewers) {
        if (this.useViewers == useViewers) {
            return;
        }
        this.useViewers = useViewers;
        this.shouldUpdate = true;
    }

    public boolean useViewers() {
        return useViewers;
    }

    public void setVisible(boolean visible) {
        synchronized (stateLock) {
            if (this.visible == visible) {
                return;
            }
            this.visible = visible;
            this.shouldUpdate = true;
        }
    }

    public void registerPlaceholder(HologramPlaceholderProvider provider, long updateIntervalTicks) {
        registerPlaceholder(provider, updateIntervalTicks, false);
    }

    /**
     * Registers a placeholder whose provider is evaluated by the asynchronous
     * hologram updater. The provider must not touch Bukkit APIs.
     */
    public void registerAsyncPlaceholder(HologramPlaceholderProvider provider, long updateIntervalTicks) {
        registerPlaceholder(provider, updateIntervalTicks, true);
    }

    private void registerPlaceholder(HologramPlaceholderProvider provider, long updateIntervalTicks, boolean async) {
        if (provider == null) {
            throw new IllegalArgumentException("Placeholder provider cannot be null");
        }
        MiniMessagePlaceholder initial = provider.getPlaceholder();
        if (initial == null || initial.getPlaceholder() == null) {
            throw new IllegalArgumentException("Placeholder provider must return a named placeholder");
        }
        synchronized (stateLock) {
            placeholders.put("<" + initial.getPlaceholder() + ">",
                    new HologramPlaceholder(provider, updateIntervalTicks, async));
            placeholderRevision++;
            shouldUpdate = true;
        }
    }

    public List<MiniMessagePlaceholder> getPlaceholders() {
        synchronized (stateLock) {
            List<MiniMessagePlaceholder> result = new ArrayList<>(placeholders.size());
            for (HologramPlaceholder placeholder : placeholders.values()) {
                result.add(placeholder.getPlaceholder());
            }
            return List.copyOf(result);
        }
    }

    private MiniMessagePlaceholder[] resolvePlaceholders() {
        return getPlaceholders().toArray(new MiniMessagePlaceholder[0]);
    }

    /**
     * Snapshot used by the packet backend.
     */
    HologramPacketSnapshot packetSnapshot() {
        synchronized (stateLock) {
            long staticRevision = Lang.getStaticPlaceholdersRevision();
            if (cachedPacketSnapshot != null
                    && cachedPacketLines == lines
                    && cachedPlaceholderRevision == placeholderRevision
                    && cachedStaticPlaceholdersRevision == staticRevision
                    && cachedPacketSnapshot.visible() == visible) {
                return cachedPacketSnapshot;
            }

            boolean canReuseComponents = cachedPlaceholderRevision == placeholderRevision
                    && cachedStaticPlaceholdersRevision == staticRevision;
            List<Component> components;
            if (canReuseComponents && cachedPacketLines == lines) {
                components = cachedPacketComponents;
            } else if (lines.isEmpty()) {
                components = List.of();
            } else {
                List<Component> updated = new ArrayList<>(lines.size());
                MiniMessagePlaceholder[] resolved = null;
                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    if (canReuseComponents
                            && index < cachedPacketLines.size()
                            && line.equals(cachedPacketLines.get(index))) {
                        updated.add(cachedPacketComponents.get(index));
                        continue;
                    }
                    if (resolved == null) {
                        List<MiniMessagePlaceholder> placeholderSnapshot = new ArrayList<>(placeholders.size());
                        for (HologramPlaceholder placeholder : placeholders.values()) {
                            placeholderSnapshot.add(placeholder.getPlaceholder());
                        }
                        resolved = placeholderSnapshot.toArray(new MiniMessagePlaceholder[0]);
                    }
                    updated.add(AdventureUtils.formatComponent(line, resolved));
                }
                components = List.copyOf(updated);
            }

            cachedPacketLines = lines;
            cachedPacketComponents = components;
            cachedPlaceholderRevision = placeholderRevision;
            cachedStaticPlaceholdersRevision = staticRevision;
            cachedPacketSnapshot = new HologramPacketSnapshot(
                    worldId,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    chunkX(),
                    chunkZ(),
                    components,
                    visible
            );
            return cachedPacketSnapshot;
        }
    }

    public synchronized void update() {
        update(false);
    }

    /**
     * Renders the hologram. With the packet backend this method may be called
     * asynchronously and only produces/sends clientbound packets. The Bukkit
     * backend retains its main-thread entity safety.
     */
    public synchronized void update(boolean force) {
        if (!force && !shouldUpdate) {
            return;
        }
        shouldUpdate = false;

        if (HologramManager.isUsingFakeArmorStands()) {
            HologramManager.updateFake(this, force);
            return;
        }

        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), location, () -> update(force));
            return;
        }
        if (!WorldUtils.isChunkLoadedAtLocation(location)) {
            return;
        }

        while (armorStands.size() > lines.size()) {
            ArmorStand armorStand = armorStands.remove(armorStands.size() - 1);
            armorStand.remove();
        }

        while (armorStands.size() < lines.size()) {
            double yOffset = (lines.size() - 1 - armorStands.size()) * 0.25;
            Location correctLocation = location.clone().add(0, yOffset, 0);
            armorStands.add(HologramManager.createArmorStand(this, correctLocation));
        }

        MiniMessagePlaceholder[] resolvedPlaceholders = resolvePlaceholders();
        for (int i = 0; i < lines.size(); i++) {
            ArmorStand armorStand = armorStands.get(i);
            String line = lines.get(i);
            double yOffset = (lines.size() - 1 - i) * 0.25;
            Location correctLocation = location.clone().add(0, yOffset, 0);

            HologramManager.registerArmorStandInWorldIfNeeded(armorStand);
            armorStand.teleportAsync(correctLocation);
            updateArmorStandName(armorStand, line, resolvedPlaceholders);
            armorStand.setCustomNameVisible(true);
        }
    }

    private void updateArmorStandName(ArmorStand armorStand, String line,
                                      MiniMessagePlaceholder[] resolvedPlaceholders) {
        armorStand.customName(AdventureUtils.formatComponent(line, resolvedPlaceholders));
    }

    public synchronized void updatePlaceholders() {
        updatePlaceholders(false);
    }

    /**
     * Refreshes normal providers while preserving their synchronous contract.
     */
    public synchronized void updatePlaceholders(boolean force) {
        if (!hasSynchronousPlaceholders()) {
            return;
        }
        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), location, () -> updatePlaceholders(force));
            return;
        }
        if (!HologramManager.isUsingFakeArmorStands() && !WorldUtils.isChunkLoadedAtLocation(location)) {
            return;
        }

        boolean anyUpdate = false;
        synchronized (stateLock) {
            for (HologramPlaceholder placeholder : placeholders.values()) {
                if (!placeholder.isAsync() && (force || placeholder.shouldUpdate(false))) {
                    placeholder.refresh();
                    anyUpdate = true;
                }
            }
            if (anyUpdate) {
                placeholderRevision++;
                shouldUpdate = true;
            }
        }
        if (anyUpdate) {
            update(true);
        }
    }

    /**
     * Refreshes only explicitly async placeholder providers off-thread.
     */
    public void updatePlaceholdersAsync(boolean force) {
        if (!hasAsyncPlaceholders()) {
            return;
        }
        HologramManager.runAsync(() -> {
            boolean anyUpdate = false;
            synchronized (stateLock) {
                for (HologramPlaceholder placeholder : placeholders.values()) {
                    if (placeholder.isAsync() && (force || placeholder.shouldUpdate(false))) {
                        placeholder.refresh();
                        anyUpdate = true;
                    }
                }
                if (anyUpdate) {
                    placeholderRevision++;
                    shouldUpdate = true;
                }
            }
            if (anyUpdate) {
                update(true);
            }
        });
    }

    private boolean hasAsyncPlaceholders() {
        synchronized (stateLock) {
            return placeholders.values().stream().anyMatch(HologramPlaceholder::isAsync);
        }
    }

    private boolean hasSynchronousPlaceholders() {
        synchronized (stateLock) {
            return placeholders.values().stream().anyMatch(placeholder -> !placeholder.isAsync());
        }
    }

    /**
     * Calculates new line contents asynchronously, then publishes the result
     * and renders it. Only the newest outstanding calculation is applied.
     */
    public void updateAsync(Supplier<List<String>> calculator) {
        if (calculator == null) {
            throw new IllegalArgumentException("Hologram calculator cannot be null");
        }

        final long sequence;
        synchronized (stateLock) {
            sequence = ++asyncUpdateSequence;
        }

        HologramManager.runAsync(() -> {
            try {
                List<String> calculatedLines = calculator.get();
                if (calculatedLines == null) {
                    return;
                }
                List<String> updatedLines = List.copyOf(calculatedLines);
                synchronized (stateLock) {
                    if (sequence != asyncUpdateSequence) {
                        return;
                    }
                    if (lines.equals(updatedLines)) {
                        return;
                    }
                    lines = updatedLines;
                    shouldUpdate = true;
                }
                update();
            } catch (Throwable throwable) {
                SongodaPlugin.getInstance().getLogger().warning(
                        "Async hologram update failed for '" + id + "': " + throwable.getMessage());
            }
        });
    }

    public List<String> getLines() {
        synchronized (stateLock) {
            return List.copyOf(lines);
        }
    }

    public void setLines(List<String> lines) {
        setLinesIfChanged(lines);
    }

    public void addLine(String line) {
        synchronized (stateLock) {
            List<String> updated = new ArrayList<>(lines);
            updated.add(line);
            lines = List.copyOf(updated);
            shouldUpdate = true;
        }
    }

    public void removeLine(int index) {
        synchronized (stateLock) {
            List<String> updated = new ArrayList<>(lines);
            updated.remove(index);
            lines = List.copyOf(updated);
            shouldUpdate = true;
        }
    }

    public void setLine(int index, String line) {
        synchronized (stateLock) {
            List<String> updated = new ArrayList<>(lines);
            updated.set(index, line);
            lines = List.copyOf(updated);
            shouldUpdate = true;
        }
    }

    /**
     * Replaces the source lines only when their contents changed.
     *
     * @return true when a new packet render is required
     */
    public boolean setLinesIfChanged(List<String> lines) {
        synchronized (stateLock) {
            List<String> updated = lines == null ? List.of() : List.copyOf(lines);
            if (this.lines.equals(updated)) {
                return false;
            }
            this.lines = updated;
            shouldUpdate = true;
            return true;
        }
    }

    public boolean canSee(UUID uuid) {
        return !useViewers || viewers.contains(uuid);
    }

    public synchronized void remove() {
        if (HologramManager.isUsingFakeArmorStands()) {
            HologramManager.removeFake(this);
            return;
        }
        if (SchedulerUtils.isFolia() && !isTickingWorlds()) {
            armorStands.clear();
            return;
        }
        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), location, this::remove);
            return;
        }
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
        armorStands.clear();
    }

    private static boolean isTickingWorlds() {
        return !ServerVersion.isAtLeastVersion(KnownServerVersions.V1_19_4) || Bukkit.isTickingWorlds();
    }

    void tickAsync() {
        update();
        updatePlaceholdersAsync(false);
        updatePlaceholders(false);
    }

    record HologramPacketSnapshot(
            UUID worldId,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            int chunkX,
            int chunkZ,
            List<Component> lines,
            boolean visible
    ) {
    }
}
