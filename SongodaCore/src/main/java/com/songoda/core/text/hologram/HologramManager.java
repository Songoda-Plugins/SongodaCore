package com.songoda.core.text.hologram;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.text.MiniMessagePlaceholder;
import com.songoda.core.text.lang.Lang;
import com.songoda.core.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HologramManager {

    /**
     * Selects the packet backend when ProtocolLib is installed and supports the
     * running server. Set this to false to force the Bukkit entity backend.
     */
    public static final boolean USE_FAKE_ARMOR_STANDS = true;
    private static final UUID sessionId = UUID.randomUUID();
    /**
     * Comma-separated UUID strings; empty string means no viewers.
     */
    private static final String VIEWERS_DELIMITER = ",";

    private static final NamespacedKey SESSION_ID_KEY = new NamespacedKey(SongodaPlugin.getInstance(), "hologram_session_id");
    private static final NamespacedKey HOLOGRAM_KEY = new NamespacedKey(SongodaPlugin.getInstance(), "hologram");
    private static final NamespacedKey VIEWERS_KEY = new NamespacedKey(SongodaPlugin.getInstance(), "hologram_viewers");

    private static final Map<Plugin, Set<Hologram>> holograms = new ConcurrentHashMap<>();
    private static final Map<HologramChunkKey, Set<Hologram>> hologramsByChunk = new ConcurrentHashMap<>();
    private static final @Nullable Method WORLD_CREATE_ENTITY;
    private static final @Nullable Method WORLD_ADD_ENTITY;
    private static final @Nullable Method ENTITY_IS_IN_WORLD;
    private static final @Nullable Method ENTITY_SET_VISIBLE_BY_DEFAULT;
    private static volatile @Nullable HologramBackend fakeArmorStandManager;

    static {
        WORLD_CREATE_ENTITY = resolveMethod(World.class, "createEntity", Location.class, Class.class);
        WORLD_ADD_ENTITY = resolveMethod(World.class, "addEntity", Entity.class);
        ENTITY_IS_IN_WORLD = resolveMethod(Entity.class, "isInWorld");
        ENTITY_SET_VISIBLE_BY_DEFAULT = resolveMethod(Entity.class, "setVisibleByDefault", boolean.class);
    }

    private static @Nullable Method resolveMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String encodeViewers(List<UUID> viewers) {
        if (viewers == null || viewers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < viewers.size(); i++) {
            if (i > 0) {
                sb.append(VIEWERS_DELIMITER);
            }
            sb.append(viewers.get(i).toString());
        }
        return sb.toString();
    }

    private static List<UUID> decodeViewers(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (String part : raw.split(VIEWERS_DELIMITER)) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip malformed token
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static void applyVisibleByDefault(Entity entity, boolean visible) {
        if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
            return;
        }
        try {
            ENTITY_SET_VISIBLE_BY_DEFAULT.invoke(entity, visible);
        } catch (ReflectiveOperationException ignored) {
            // If the runtime rejects the call, fall back to per-player hide/show only
        }
    }

    /**
     * Per-viewer visibility: uses {@code setVisibleByDefault} when the server API exposes it; otherwise
     * {@link Player#hideEntity}/{@link Player#showEntity} for every online player.
     */
    private static void applyHologramViewerVisibility(ArmorStand stand, Hologram hologram) {
        if (hologram.useViewers()) {
            applyVisibleByDefault(stand, false);
            for (UUID uuid : hologram.getViewers()) {
                updatePlayerVisibility(uuid, stand, true);
            }
            if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!hologram.getViewers().contains(player.getUniqueId())) {
                        updatePlayerVisibility(player.getUniqueId(), stand, false);
                    }
                }
            }
        } else {
            applyVisibleByDefault(stand, true);
            if (ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerVisibility(player.getUniqueId(), stand, true);
                }
            }
        }
    }

    private static void updatePlayerVisibility(UUID playerId, ArmorStand stand, boolean visible) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        Runnable updateVisibility = () -> {
            if (!player.isOnline()) {
                return;
            }
            if (visible) {
                player.showEntity(SongodaPlugin.getInstance(), stand);
            } else {
                player.hideEntity(SongodaPlugin.getInstance(), stand);
            }
        };

        if (SchedulerUtils.isOwnedByCurrentRegion(player)) {
            updateVisibility.run();
        } else {
            SchedulerUtils.runEntityTask(SongodaPlugin.getInstance(), player, updateVisibility);
        }
    }

    /**
     * When a player joins, re-apply viewer rules (needed for viewer lists and for servers without
     * {@code setVisibleByDefault}).
     */
    public static void onPlayerJoin(Player player) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.onPlayerJoin(player);
            return;
        }
        Set<Hologram> set = holograms.get(SongodaPlugin.getInstance());
        if (set == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        for (Hologram hologram : set) {
            if (!hologram.useViewers()) {
                continue;
            }
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), hologram.getLocation(), () -> {
                boolean visible = hologram.getViewers().contains(uuid);
                if (visible || ENTITY_SET_VISIBLE_BY_DEFAULT == null) {
                    for (ArmorStand stand : hologram.getArmorStands()) {
                        updatePlayerVisibility(uuid, stand, visible);
                    }
                }
            });
        }
    }

    public static void onPlayerQuit(UUID playerId) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.onPlayerQuit(playerId);
        }
    }

    public static void onPlayerChangedWorld(Player player) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.onPlayerChangedWorld(player);
        }
    }

    /**
     * Registers a stand created via {@code World#createEntity} (1.20.3+). No-op when spawn was used or the API is absent.
     */
    public static void registerArmorStandInWorldIfNeeded(ArmorStand stand) {
        if (!ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_3)) {
            return;
        }
        if (WORLD_ADD_ENTITY == null || ENTITY_IS_IN_WORLD == null) {
            return;
        }
        try {
            if (!(Boolean) ENTITY_IS_IN_WORLD.invoke(stand)) {
                WORLD_ADD_ENTITY.invoke(stand.getWorld(), stand);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prefer {@code World#createEntity} on 1.20.3+ when present; otherwise {@link World#spawn(Location, Class)}.
     */
    private static ArmorStand createArmorStandEntity(Location location, Consumer<ArmorStand> configure) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }
        if (ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_3) && WORLD_CREATE_ENTITY != null) {
            try {
                Object created = WORLD_CREATE_ENTITY.invoke(world, location, ArmorStand.class);
                ArmorStand stand = (ArmorStand) created;
                configure.accept(stand);
                return stand;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        ArmorStand armorStand = world.spawn(location, ArmorStand.class);
        configure.accept(armorStand);
        return armorStand;
    }

    public static void init() {
        clear();

        if (USE_FAKE_ARMOR_STANDS && Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            try {
                Class<?> backendClass = Class.forName(
                        "com.songoda.core.text.hologram.FakeArmorStandManager");
                var constructor = backendClass.getDeclaredConstructor(Plugin.class);
                constructor.setAccessible(true);
                HologramBackend candidate = (HologramBackend) constructor.newInstance(
                        SongodaPlugin.getInstance());
                if (candidate.isSupported()) {
                    candidate.init();
                    fakeArmorStandManager = candidate;
                    SongodaPlugin.getInstance().getLogger().info(
                            "Using ProtocolLib fake armor stands for holograms.");
                } else {
                    SongodaPlugin.getInstance().getLogger().warning(
                            "ProtocolLib is available, but fake hologram packets are unsupported on this server. "
                                    + "Falling back to Bukkit armor stands.");
                }
            } catch (Throwable throwable) {
                SongodaPlugin.getInstance().getLogger().warning(
                        "Could not initialize ProtocolLib fake holograms. Falling back to Bukkit armor stands: "
                                + throwable.getMessage());
            }
        } else if (USE_FAKE_ARMOR_STANDS) {
            SongodaPlugin.getInstance().getLogger().info(
                    "ProtocolLib is not enabled; using Bukkit armor stands for holograms.");
        }

        //Create a bukkit scheduler task to tick all holograms
        SchedulerUtils.runTaskTimerAsynchronously(SongodaPlugin.getInstance(), () -> {
            for (Set<Hologram> hologramsSet : holograms.values()) {
                for (Hologram hologram : hologramsSet) {
                    hologram.tickAsync();
                }
            }
        }, 0, 10L);
    }

    static void runAsync(Runnable runnable) {
        SchedulerUtils.runTaskAsynchronously(SongodaPlugin.getInstance(), runnable);
    }

    static boolean isUsingFakeArmorStands() {
        return fakeArmorStandManager != null;
    }

    static void updateFake(Hologram hologram, boolean force) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            if (Bukkit.isPrimaryThread()) {
                SchedulerUtils.runTaskAsynchronously(SongodaPlugin.getInstance(), () -> {
                    if (fakeArmorStandManager == fakeManager
                            && getHologramsView().contains(hologram)) {
                        fakeManager.render(hologram, force);
                    }
                });
            } else {
                fakeManager.render(hologram, force);
            }
        }
    }

    static void removeFake(Hologram hologram) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            if (Bukkit.isPrimaryThread()) {
                SchedulerUtils.runTaskAsynchronously(SongodaPlugin.getInstance(), () -> {
                    if (fakeArmorStandManager == fakeManager) {
                        fakeManager.remove(hologram);
                    }
                });
            } else {
                fakeManager.remove(hologram);
            }
        }
    }

    static Set<Hologram> getHologramsView() {
        return holograms.getOrDefault(SongodaPlugin.getInstance(), Set.of());
    }

    static Set<Hologram> getHologramsInChunk(UUID worldId, int chunkX, int chunkZ) {
        return hologramsByChunk.getOrDefault(new HologramChunkKey(worldId, chunkX, chunkZ), Set.of());
    }

    public static @Nullable Hologram getHologram(String id) {
        Set<Hologram> hologramsSet = holograms.get(SongodaPlugin.getInstance());
        if (hologramsSet != null) {
            for (Hologram hologram : hologramsSet) {
                if (hologram.getId().equals(id)) {
                    return hologram;
                }
            }
        }
        return null;
    }

    public static void createHologram(Hologram hologram) {
        Location location = hologram.getLocation();
        boolean added = holograms.computeIfAbsent(SongodaPlugin.getInstance(), k -> ConcurrentHashMap.newKeySet())
                .add(hologram);
        if (added) {
            hologramsByChunk.computeIfAbsent(HologramChunkKey.of(hologram), ignored -> ConcurrentHashMap.newKeySet())
                    .add(hologram);
        }

        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            updateFake(hologram, true);
            return;
        }

        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), location, () -> createHologram(hologram));
            return;
        }

        // Do not create hologram if the chunk is not loaded
        if (!WorldUtils.isChunkLoadedAtLocation(location)) return;

        List<MiniMessagePlaceholder> placeholders = new ArrayList<>(hologram.getPlaceholders());
        placeholders.addAll(Lang.staticPlaceholders);

        //Summon the hologram to the world
        // We need one armor stand for each line, use a gap of 0.25 blocks for each line
        // First line (index 0) should be at the top, so we reverse the Y offset
        int lineCount = hologram.getLines().size();
        for (int i = 0; i < lineCount; i++) {
            double yOffset = (lineCount - 1 - i) * 0.25;
            Location lineLoc = location.clone().add(0, yOffset, 0);
            final int lineIndex = i;
            ArmorStand armorStand = createArmorStandEntity(lineLoc, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setCollidable(false);
                stand.setPersistent(false);

                stand.customName(AdventureUtils.formatComponent(hologram.getLines().get(lineIndex), placeholders));
                stand.setCustomNameVisible(true);

                PersistentDataContainer data = stand.getPersistentDataContainer();
                data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
                data.set(SESSION_ID_KEY, PersistentDataType.STRING, sessionId.toString());
                if (hologram.useViewers()) {
                    data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
                }
                applyHologramViewerVisibility(stand, hologram);
            });

            registerArmorStandInWorldIfNeeded(armorStand);
            hologram.getArmorStands().add(armorStand);
        }
    }

    public static ArmorStand createArmorStand(Hologram hologram) {
        return createArmorStand(hologram, hologram.getLocation());
    }

    /**
     * Create an armor stand for the hologram at the given location without adding it to the world
     *
     * @param hologram The hologram to create the armor stand for
     * @param location The location to create the armor stand at
     * @return The created armor stand
     */
    public static ArmorStand createArmorStand(Hologram hologram, Location location) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            if (SchedulerUtils.isFolia()) {
                throw new IllegalStateException("Armor stands must be created on the region that owns the location.");
            }
            Callable<ArmorStand> callable = () -> createArmorStand(hologram, location);
            try {
                return Bukkit.getScheduler().callSyncMethod(SongodaPlugin.getInstance(), callable).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ArmorStand armorStand = createArmorStandEntity(location, stand -> {
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setCollidable(false);
            stand.setPersistent(false);

            PersistentDataContainer data = stand.getPersistentDataContainer();
            data.set(HOLOGRAM_KEY, PersistentDataType.STRING, hologram.getId());
            if (hologram.useViewers()) {
                data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
            }
            applyHologramViewerVisibility(stand, hologram);
        });

        return armorStand;
    }

    public static void removeHologram(Hologram hologram) {
        Set<Hologram> hologramsSet = holograms.get(SongodaPlugin.getInstance());
        if (hologramsSet != null) {
            if (hologramsSet.remove(hologram)) {
                HologramChunkKey key = HologramChunkKey.of(hologram);
                Set<Hologram> chunkHolograms = hologramsByChunk.get(key);
                if (chunkHolograms != null) {
                    chunkHolograms.remove(hologram);
                    if (chunkHolograms.isEmpty()) {
                        hologramsByChunk.remove(key, chunkHolograms);
                    }
                }
            }
            hologram.remove();
        }
    }

    public static void clear() {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.clear();
            fakeArmorStandManager = null;
            holograms.clear();
            hologramsByChunk.clear();
            return;
        }
        Set<Hologram> registeredHolograms = getHologramsView();
        for (Hologram hologram : registeredHolograms) {
            hologram.remove();
        }
        holograms.clear();
        hologramsByChunk.clear();
    }

    public static void loadHologramsInChunk(Chunk chunk) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.onServerChunkLoad(chunk);
            return;
        }
        for (Hologram hologram : getHologramsInChunk(
                chunk.getWorld().getUID(), chunk.getX(), chunk.getZ())) {
            createHologram(hologram);
        }
    }

    public static void unloadHologramsInChunk(Chunk chunk) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            fakeManager.onServerChunkUnload(chunk);
            return;
        }
        for (Hologram hologram : getHologramsInChunk(
                chunk.getWorld().getUID(), chunk.getX(), chunk.getZ())) {
            hologram.remove();
        }
    }

    public static NamespacedKey getHologramKey() {
        return HOLOGRAM_KEY;
    }

    public static NamespacedKey getSessionIdKey() {
        return SESSION_ID_KEY;
    }

    public static String getSessionId() {
        return sessionId.toString();
    }

    public void updateViewers(Hologram hologram) {
        HologramBackend fakeManager = fakeArmorStandManager;
        if (fakeManager != null) {
            updateFake(hologram, true);
            return;
        }
        Location location = hologram.getLocation();
        if (!SchedulerUtils.isOwnedByCurrentRegion(location)) {
            SchedulerUtils.runLocationTask(SongodaPlugin.getInstance(), location, () -> updateViewers(hologram));
            return;
        }
        for (ArmorStand armorStand : hologram.getArmorStands()) {
            PersistentDataContainer data = armorStand.getPersistentDataContainer();
            String rawOld = data.get(VIEWERS_KEY, PersistentDataType.STRING);
            List<UUID> oldViewers = decodeViewers(rawOld);
            for (UUID uuid : oldViewers) {
                updatePlayerVisibility(uuid, armorStand, false);
            }

            if (hologram.useViewers()) {
                data.set(VIEWERS_KEY, PersistentDataType.STRING, encodeViewers(hologram.getViewers()));
            } else {
                data.remove(VIEWERS_KEY);
            }
            applyHologramViewerVisibility(armorStand, hologram);
        }
    }

    private record HologramChunkKey(UUID worldId, int x, int z) {
        private static HologramChunkKey of(Hologram hologram) {
            return new HologramChunkKey(hologram.worldId(), hologram.chunkX(), hologram.chunkZ());
        }
    }
}
