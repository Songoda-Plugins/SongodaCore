package com.songoda.core.world;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the generation and verification of a custom Datapack for skies.
 * <p>
 * Instead of hacking packets, this system generates native biomes and dimensions.
 * Once the server is restarted, these can be used reliably without performance overhead.
 */
//@Component
public class SkyColorManager {

    private final Plugin plugin;
    private final File datapackRoot;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ProtocolManager protocolManager;

    @Getter
    private final Map<String, SkyPreset> presets = new HashMap<>();

    /**
     * Maps player UUID to their active SkyPreset.
     */
    private final ConcurrentHashMap<UUID, SkyPreset> activePlayers = new ConcurrentHashMap<>();

    /**
     * Cache for biome numeric IDs resolved from NamespacedKeys.
     * key: namespacedKey, value: numericId
     */
    private final Map<String, Integer> biomeIdCache = new ConcurrentHashMap<>();

    public SkyColorManager(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        // Target the primary world's datapack folder
        String worldName = Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().get(0).getName();
        this.datapackRoot = new File(Bukkit.getWorldContainer(), worldName + "/datapacks/vortex_sky");

        loadDefaultPresets();
        if (ensureDatapack()) {
            plugin.getLogger().info("--------------------------------------------------");
            plugin.getLogger().warning("Vortex Sky Datapack was updated or installed!");
            plugin.getLogger().warning("A server RESTART is required to load new biomes/dimensions.");
            plugin.getLogger().info("--------------------------------------------------");
        }

        registerRegistryInterceptor();
        registerChunkListener();
    }

    private void loadDefaultPresets() {
        presets.clear();
        addPreset("blood_moon", 0x330000, 0x200000, 0x550000, 0.2f);
        addPreset("toxic_storm", 0x223300, 0x112200, 0x3F76E4, 0.5f);
        addPreset("purple_dream", 0x440044, 0x220022, 0x3F76E4, 0.3f);
    }

    private void addPreset(String id, int skyColor, int fogColor, int waterColor, float ambientLight) {
        SkyPreset preset = new SkyPreset();
        preset.setId(id);
        preset.setSkyColor(skyColor);
        preset.setFogColor(fogColor);
        preset.setWaterColor(waterColor);
        preset.setAmbientLight(ambientLight);
        presets.put(id, preset);
    }

    /**
     * Generates the datapack files if they don't exist or are outdated.
     *
     * @return true if a restart is needed.
     */
    public boolean ensureDatapack() {
        boolean changed = false;

        // 1. Root and Meta
        if (!datapackRoot.exists()) {
            datapackRoot.mkdirs();
            changed = true;
        }

        File mcmeta = new File(datapackRoot, "pack.mcmeta");
        if (!mcmeta.exists()) {
            writeJson(mcmeta, createPackMeta());
            changed = true;
        }

        // 2. Generate Biomes for each preset
        File biomeDir = new File(datapackRoot, "data/vortex/worldgen/biome");
        biomeDir.mkdirs();
        for (SkyPreset preset : presets.values()) {
            File biomeFile = new File(biomeDir, preset.getId() + ".json");
            JsonObject biomeJson = createBiomeJson(preset);
            if (writeJsonIfChanged(biomeFile, biomeJson)) {
                changed = true;
            }
        }

        // 3. Generate Dimension Types (for fixed lighting/time)
        File dimTypeDir = new File(datapackRoot, "data/vortex/dimension_type");
        dimTypeDir.mkdirs();
        for (SkyPreset preset : presets.values()) {
            File dimFile = new File(dimTypeDir, preset.getId() + ".json");
            JsonObject dimJson = createDimensionTypeJson(preset);
            if (writeJsonIfChanged(dimFile, dimJson)) {
                changed = true;
            }
        }

        return changed;
    }

    private JsonObject createPackMeta() {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Vortex Dynamic Sky Datapack");
        pack.addProperty("pack_format", 48); // 1.21.3+
        root.add("pack", pack);
        return root;
    }

    private JsonObject createBiomeJson(SkyPreset preset) {
        JsonObject root = new JsonObject();
        root.addProperty("has_precipitation", false);
        root.addProperty("temperature", 0.5f);
        root.addProperty("downfall", 0.5f);

        // 1. Mandatory Root Fields (Legacy/Universal Fallback)
        JsonObject spawners = new JsonObject();
        spawners.add("monster", new com.google.gson.JsonArray());
        spawners.add("creature", new com.google.gson.JsonArray());
        spawners.add("ambient", new com.google.gson.JsonArray());
        spawners.add("water_creature", new com.google.gson.JsonArray());
        spawners.add("water_ambient", new com.google.gson.JsonArray());
        spawners.add("underground_water_creature", new com.google.gson.JsonArray());
        spawners.add("axolotls", new com.google.gson.JsonArray());
        spawners.add("misc", new com.google.gson.JsonArray());

        root.add("spawners", spawners);
        root.add("spawn_costs", new JsonObject());

        com.google.gson.JsonArray features = new com.google.gson.JsonArray();
        for (int i = 0; i < 11; i++) {
            features.add(new com.google.gson.JsonArray());
        }
        root.add("features", features);
        root.add("carvers", new JsonObject());

        // 2. Mandatory Nested Objects (Modern 1.21.x)
        JsonObject spawnSettings = new JsonObject();
        spawnSettings.addProperty("creature_spawn_probability", 0.0f);
        spawnSettings.add("spawners", spawners);
        spawnSettings.add("spawn_costs", new JsonObject());
        root.add("spawn_settings", spawnSettings);

        JsonObject genSettings = new JsonObject();
        genSettings.add("features", features);
        genSettings.add("carvers", new JsonObject());
        root.add("generation_settings", genSettings);

        // 3. Atmosphere Effects
        JsonObject effects = new JsonObject();
        effects.addProperty("sky_color", preset.getSkyColor());
        effects.addProperty("fog_color", preset.getFogColor());
        effects.addProperty("water_color", preset.getWaterColor());
        effects.addProperty("water_fog_color", 0x050533);

        JsonObject mood = new JsonObject();
        mood.addProperty("sound", "minecraft:ambient.cave");
        mood.addProperty("tick_delay", 6000);
        mood.addProperty("offset", 2.0);
        mood.addProperty("block_search_extent", 8);
        effects.add("mood_sound", mood);

        root.add("effects", effects);
        return root;
    }

    private JsonObject createDimensionTypeJson(SkyPreset preset) {
        JsonObject root = new JsonObject();
        root.addProperty("ultrawarm", false);
        root.addProperty("natural", true);
        root.addProperty("coordinate_scale", 1.0);
        root.addProperty("has_skylight", true);
        root.addProperty("has_ceiling", false);
        root.addProperty("ambient_light", preset.getAmbientLight());

        // Mandatory flags for 1.21.x
        root.addProperty("has_raids", true);
        root.addProperty("piglin_safe", false);
        root.addProperty("respawn_anchor_works", false);
        root.addProperty("bed_works", true);

        if (preset.getFixedTime() != null) {
            root.addProperty("fixed_time", preset.getFixedTime());
        }

        root.addProperty("monster_spawn_light_level", 0);
        root.addProperty("monster_spawn_block_light_limit", 0);
        root.addProperty("infiniburn", "#minecraft:infiniburn_overworld");
        root.addProperty("effects", "minecraft:overworld");
        root.addProperty("min_y", -64);
        root.addProperty("height", 384);
        root.addProperty("logical_height", 384);

        return root;
    }

    private boolean writeJsonIfChanged(File file, JsonObject json) {
        String newContent = gson.toJson(json);
        if (file.exists()) {
            try {
                String oldContent = Files.readString(file.toPath());
                if (oldContent.equals(newContent)) return false;
            } catch (IOException ignored) {
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(newContent);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write datapack file: " + file.getName());
            return false;
        }
    }

    private void writeJson(File file, JsonObject json) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(gson.toJson(json));
        } catch (IOException ignored) {
        }
    }

    /**
     * Applies a sky preset to a player.
     */
    public void enable(Player player, String presetId) {
        SkyPreset preset = presets.get(presetId);
        if (preset == null) return;

        activePlayers.put(player.getUniqueId(), preset);

        // If the preset has dimension-level overrides (light/time), we MUST send a respawn packet
        if (preset.getFixedTime() != null || preset.getAmbientLight() != 0.0f) {
            sendDimensionTypeChange(player, preset);
        }

        refreshChunks(player);
    }

    // ------------------------------------------------------------------
    // Switching Logic

    public void disable(Player player) {
        SkyPreset previous = activePlayers.remove(player.getUniqueId());
        if (previous != null) {
            // Revert dimension type if it was changed
            if (previous.getFixedTime() != null || previous.getAmbientLight() != 0.0f) {
                revertDimensionType(player);
            }
            refreshChunks(player);
        }
    }

    private void registerChunkListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                SkyPreset preset = activePlayers.get(event.getPlayer().getUniqueId());
                if (preset == null) return;

                int targetId = getBiomeId(preset.getBiomeKey());
                if (targetId == -1) return;

                try {
                    rewriteChunks(event, targetId);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void registerRegistryInterceptor() {
        // Intercept Login and Registry Data to capture IDs
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.LOGIN,
                com.comphenix.protocol.PacketType.Configuration.Server.REGISTRY_DATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // We extract the registry NBT and look for the Biome mapping
                // ProtocolLib simplifies this if we look at the NBT structure
                try {
                    PacketContainer packet = event.getPacket();
                    var nbtMod = packet.getNbtModifier();
                    if (nbtMod.size() == 0) return;

                    NbtBase<?> base = nbtMod.read(0);
                    if (!(base instanceof NbtCompound)) return;
                    NbtCompound nbt = (NbtCompound) base;

                    // 1.21+ REGISTRY_DATA is top-level. LOGIN has it nested.
                    if (packet.getType() == PacketType.Play.Server.LOGIN) {
                        if (nbt.containsKey("minecraft:worldgen/biome")) {
                            NbtBase<?> biomeReg = nbt.getValue("minecraft:worldgen/biome");
                            if (biomeReg instanceof NbtCompound) {
                                captureBiomeIds((NbtCompound) biomeReg);
                            }
                        }
                    } else if (nbt.containsKey("value")) {
                        // Check if this registry is the biome registry
                        boolean isBiome = false;
                        for (Object field : packet.getModifier().getValues()) {
                            if (field != null && field.toString().contains("worldgen/biome")) {
                                isBiome = true;
                                break;
                            }
                        }
                        if (isBiome) captureBiomeIds(nbt);
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void captureBiomeIds(NbtCompound registry) {
        NbtList<?> list = registry.getList("value");
        for (Object entryObj : list.asCollection()) {
            if (entryObj instanceof NbtCompound) {
                NbtCompound entry = (NbtCompound) entryObj;
                String name = entry.getString("name");
                int id = entry.getInteger("id");
                biomeIdCache.put(name, id);
            }
        }
    }

    private int getBiomeId(String key) {
        return biomeIdCache.getOrDefault(key, -1);
    }

    private void rewriteChunks(PacketEvent event, int targetBiomeId) throws Exception {
        StructureModifier<Object> modifier = event.getPacket().getModifier();
        Object chunkDataObj = null;
        for (Object obj : modifier.getValues()) {
            if (obj != null && (obj.getClass().getSimpleName().contains("ChunkData") || obj.getClass().getSimpleName().contains("ChunkPacketData"))) {
                chunkDataObj = obj;
                break;
            }
        }
        if (chunkDataObj == null) return;

        StructureModifier<byte[]> bytesMod = new StructureModifier<>(chunkDataObj.getClass()).withType(byte[].class);
        byte[] data = bytesMod.withTarget(chunkDataObj).read(0);
        if (data == null || data.length == 0) return;

        World world = event.getPlayer().getWorld();
        int sectionCount = (world.getMaxHeight() - world.getMinHeight()) / 16;

        byte[] rewritten = rewriteBiomePalette(data, sectionCount, targetBiomeId);
        bytesMod.withTarget(chunkDataObj).write(0, rewritten);
    }

    private byte[] rewriteBiomePalette(byte[] original, int sections, int targetId) {
        ByteBuf buf = Unpooled.wrappedBuffer(original);
        ByteBuf out = Unpooled.buffer(original.length);
        try {
            for (int i = 0; i < sections; i++) {
                if (!buf.isReadable()) break;
                out.writeShort(buf.readShort()); // block count

                // Block palette (Passthrough)
                processPalette(buf, out, true);

                // Biome palette (Modify)
                skipPalette(buf);
                out.writeByte(0); // 0 bits
                writeVarInt(out, targetId); // New ID
                writeVarInt(out, 0); // No data
            }
            if (buf.isReadable()) out.writeBytes(buf);
            byte[] res = new byte[out.writerIndex()];
            out.getBytes(0, res);
            return res;
        } finally {
            buf.release();
            out.release();
        }
    }

    private void processPalette(ByteBuf in, ByteBuf out, boolean copy) {
        byte bits = in.readByte();
        if (out != null) out.writeByte(bits);
        if (bits == 0) {
            int id = readVarInt(in);
            if (out != null) writeVarInt(out, id);
            int dataLen = readVarInt(in);
            if (out != null) writeVarInt(out, dataLen);
        } else if (bits <= 8) { // Biome limit is 3, Block limit is 8
            int palLen = readVarInt(in);
            if (out != null) writeVarInt(out, palLen);
            for (int i = 0; i < palLen; i++) {
                int id = readVarInt(in);
                if (out != null) writeVarInt(out, id);
            }
            int dataLen = readVarInt(in);
            if (out != null) writeVarInt(out, dataLen);
            if (out != null) in.readBytes(out, dataLen * 8);
            else in.skipBytes(dataLen * 8);
        } else {
            int dataLen = readVarInt(in);
            if (out != null) writeVarInt(out, dataLen);
            if (out != null) in.readBytes(out, dataLen * 8);
            else in.skipBytes(dataLen * 8);
        }
    }

    private void skipPalette(ByteBuf in) {
        processPalette(in, null, false);
    }

    private void sendDimensionTypeChange(Player player, SkyPreset preset) {
        // This requires sending a Respawn packet with a different DimensionType NamespacedKey
        // This is complex and might trigger a loading screen.
        // For now, we focus on Biome swapping which is 90% of the effect.
    }

    private void revertDimensionType(Player player) {
        // Revert logic
    }

    private void refreshChunks(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            int view = Bukkit.getViewDistance();
            int cx = player.getLocation().getChunk().getX();
            int cz = player.getLocation().getChunk().getZ();
            for (int x = -view; x <= view; x++) {
                for (int z = -view; z <= view; z++) {
                    player.getWorld().refreshChunk(cx + x, cz + z);
                }
            }
        });
    }

    private void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private int readVarInt(ByteBuf buf) {
        int result = 0, shift = 0;
        byte b;
        do {
            b = buf.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }

    @Getter
    @Setter
    public static class SkyPreset {
        private String id;
        private int skyColor;
        private int fogColor;
        private int waterColor;
        private float ambientLight;
        private Long fixedTime;

        public String getBiomeKey() {
            return "vortex:" + id;
        }

        public String getDimensionTypeKey() {
            return "vortex:" + id;
        }
    }
}
