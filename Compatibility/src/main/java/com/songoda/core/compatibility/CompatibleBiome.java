package com.songoda.core.compatibility;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Biomes that are compatible with server versions 1.7+
 */
public enum CompatibleBiome {
    /* 1.19 */
    MANGROVE_SWAMP(ServerVersion.V1_19),
    DEEP_DARK(ServerVersion.V1_19),

    /* 1.17 */
    DRIPSTONE_CAVES(ServerVersion.V1_17),
    LUSH_CAVES(ServerVersion.V1_17),

    /* 1.16 */
    SOUL_SAND_VALLEY(ServerVersion.V1_16),
    CRIMSON_FOREST(ServerVersion.V1_16),
    WARPED_FOREST(ServerVersion.V1_16),
    BASALT_DELTAS(ServerVersion.V1_16),

    BADLANDS(ServerVersion.V1_13, v("MESA")),
    BADLANDS_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "MESA_CLEAR_ROCK"), v("MESA_PLATEAU")),
    BAMBOO_JUNGLE(ServerVersion.V1_14),
    BAMBOO_JUNGLE_HILLS(ServerVersion.V1_14),
    BEACH(ServerVersion.V1_13, v(ServerVersion.V1_9, "BEACHES")),
    BIRCH_FOREST(),
    BIRCH_FOREST_HILLS(),
    COLD_OCEAN(ServerVersion.V1_13),
    DARK_FOREST(ServerVersion.V1_13, v("ROOFED_FOREST")),
    DARK_FOREST_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_ROOFED_FOREST")),
    DEEP_COLD_OCEAN(ServerVersion.V1_13),
    DEEP_FROZEN_OCEAN(ServerVersion.V1_13),
    DEEP_LUKEWARM_OCEAN(ServerVersion.V1_13),
    DEEP_OCEAN(),
    DEEP_WARM_OCEAN(ServerVersion.V1_13),
    DESERT(),
    DESERT_HILLS(),
    DESERT_LAKES(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_DESERT")),
    END_BARRENS(ServerVersion.V1_13),
    END_HIGHLANDS(ServerVersion.V1_13),
    END_MIDLANDS(ServerVersion.V1_13),
    ERODED_BADLANDS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_MESA")),
    FLOWER_FOREST(),
    FOREST(),
    FROZEN_OCEAN(),
    FROZEN_RIVER(),
    GIANT_SPRUCE_TAIGA(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_REDWOOD_TAIGA")),
    GIANT_SPRUCE_TAIGA_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_REDWOOD_TAIGA_HILLS")),
    GIANT_TREE_TAIGA(ServerVersion.V1_13, v(ServerVersion.V1_9, "REDWOOD_TAIGA")),
    GIANT_TREE_TAIGA_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "REDWOOD_TAIGA_HILLS")),
    GRAVELLY_MOUNTAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_EXTREME_HILLS ")),
    ICE_SPIKES(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_ICE_FLATS")),
    JUNGLE(),
    JUNGLE_EDGE(),
    JUNGLE_HILLS(),
    LUKEWARM_OCEAN(),
    MODIFIED_BADLANDS_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_MESA_CLEAR_ROCK")),
    MODIFIED_GRAVELLY_MOUNTAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_EXTREME_HILLS_WITH_TREES"), v("EXTREME_HILLS_MOUNTAINS")),
    MODIFIED_JUNGLE(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_JUNGLE"), v("JUNGLE_MOUNTAINS")),
    MODIFIED_JUNGLE_EDGE(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_JUNGLE_EDGE"), v("JUNGLE_EDGE_MOUNTAINS")),
    MODIFIED_WOODED_BADLANDS_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_MESA_ROCK"), v("MESA_PLATEAU_FOREST_MOUNTAINS")),
    MOUNTAINS(ServerVersion.V1_13, v("EXTREME_HILLS")),
    MOUNTAIN_EDGE(ServerVersion.V1_13, v(ServerVersion.V1_9, "SMALLER_EXTREME_HILLS")),
    MUSHROOM_FIELDS(ServerVersion.V1_13, v("MUSHROOM_ISLAND")),
    MUSHROOM_FIELD_SHORE(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUSHROOM_ISLAND_SHORE"), v("MUSHROOM_SHORE")),
    NETHER_WASTES(ServerVersion.V1_16, v(ServerVersion.V1_13, "NETHER"), v("HELL")),
    OCEAN(),
    PLAINS(),
    RIVER(),
    SAVANNA(),
    SAVANNA_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "SAVANNA_ROCK"), v("SAVANNA_PLATEAU")),
    SHATTERED_SAVANNA(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_SAVANNA"), v("SAVANNA_MOUNTAINS")),
    SHATTERED_SAVANNA_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_SAVANNA_ROCK"), v("SAVANNA_PLATEAU_MOUNTAINS")),
    SMALL_END_ISLANDS(ServerVersion.V1_13),
    SNOWY_BEACH(ServerVersion.V1_13, v("COLD_BEACH")),
    SNOWY_MOUNTAINS(ServerVersion.V1_13, v("ICE_MOUNTAINS")),
    SNOWY_TAIGA(ServerVersion.V1_13, v(ServerVersion.V1_9, "TAIGA_COLD")),
    SNOWY_TAIGA_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "TAIGA_COLD_HILLS"), v("COLD_TAIGA_HILLS")),
    SNOWY_TAIGA_MOUNTAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_TAIGA_COLD"), v("COLD_TAIGA_MOUNTAINS")),
    SNOWY_TUNDRA(ServerVersion.V1_13, v(ServerVersion.V1_9, "ICE_FLATS")),
    STONE_SHORE(ServerVersion.V1_13, v("STONE_BEACH")),
    SUNFLOWER_PLAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_PLAINS"), v("SUNFLOWER_PLAINS")),
    SWAMP(ServerVersion.V1_13, v("SWAMPLAND")),
    SWAMP_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_SWAMPLAND"), v("SWAMPLAND_MOUNTAINS")),
    TAIGA(),
    TAIGA_HILLS(),
    TAIGA_MOUNTAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "TAIGA_MOUNTAINS"), v("MUTATED_TAIGA")),
    TALL_BIRCH_FOREST(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_BIRCH_FOREST"), v("BIRCH_FOREST_MOUNTAINS")),
    TALL_BIRCH_HILLS(ServerVersion.V1_13, v(ServerVersion.V1_9, "MUTATED_BIRCH_FOREST_HILLS"), v("MESA_PLATEAU_FOREST_MOUNTAINS")),
    THE_END(ServerVersion.V1_13, v("SKY")),
    THE_VOID(ServerVersion.V1_13, v(ServerVersion.V1_9, "VOID")),
    WARM_OCEAN(ServerVersion.V1_13),
    WOODED_BADLANDS_PLATEAU(ServerVersion.V1_13, v(ServerVersion.V1_9, "MESA_ROCK"), v("MESA_PLATEAU_FOREST")),
    WOODED_HILLS(ServerVersion.V1_13, v("FOREST_HILLS")),
    WOODED_MOUNTAINS(ServerVersion.V1_13, v(ServerVersion.V1_9, "EXTREME_HILLS_WITH_TREES"), v("EXTREME_HILLS_PLUS"));

    private static final boolean isAbove1_16_R1 = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_16)
            && !ServerVersion.getServerVersionString().equals("v1_16_R1");

    private static final Map<String, CompatibleBiome> lookupMap = new HashMap<>();
    private static final Set<CompatibleBiome> compatibleBiomes = new HashSet<>();
    private final Deque<Version> versions = new ArrayDeque<>();
    private static Method methodGetHandle, methodMarkDirty, methodBiomeToBiomeBase, methodGetBiomeIndex, methodSetBiome;
    private static Field fieldStorageRegistry;

    static {
        for (CompatibleBiome biome : values()) {
            for (Version version : biome.getVersions()) {
                lookupMap.put(version.biome, biome);
            }
        }

        for (CompatibleBiome biome : CompatibleBiome.values()) {
            if (biome.isCompatible()) {
                compatibleBiomes.add(biome);
            }
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15) && ServerVersion.isServerVersionBelow(ServerVersion.V1_18)) {
            try {
                Class<?> classBiomeBase = ClassMapping.BIOME_BASE.getClazz(),
                        classCraftChunk = ClassMapping.CRAFT_CHUNK.getClazz(),
                        classCraftBlock = ClassMapping.CRAFT_BLOCK.getClazz(),
                        classChunk = ClassMapping.CHUNK.getClazz(),
                        classBiomeStorage = ClassMapping.BIOME_STORAGE.getClazz(),
                        classIRegistry = ClassMapping.I_REGISTRY.getClazz();

                methodBiomeToBiomeBase = isAbove1_16_R1 ? classCraftBlock.getMethod("biomeToBiomeBase", classIRegistry, Biome.class)
                        : classCraftBlock.getMethod("biomeToBiomeBase", Biome.class);
                methodGetHandle = classCraftChunk.getMethod("getHandle");
                methodMarkDirty = classChunk.getMethod("markDirty");
                methodGetBiomeIndex = classChunk.getMethod("getBiomeIndex");
                methodSetBiome = classBiomeStorage.getMethod("setBiome", int.class, int.class, int.class, classBiomeBase);

                try {
                    // If 1.16.5
                    fieldStorageRegistry = classBiomeStorage.getDeclaredField("registry");
                } catch (NoSuchFieldException ex) {
                    // If less than 1.16.5
                    fieldStorageRegistry = classBiomeStorage.getDeclaredField("g");
                }

                fieldStorageRegistry.setAccessible(true);
            } catch (NoSuchMethodException | NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }
    }

    CompatibleBiome() {
        this.versions.add(v(ServerVersion.UNKNOWN, name()));
    }

    CompatibleBiome(ServerVersion version, Version... versions) {
        this.versions.add(v(version, name()));
        this.versions.addAll(Arrays.asList(versions));
    }

    public boolean isCompatible() {
        return getBiome() != null;
    }

    public List<Version> getVersions() {
        return new LinkedList<>(this.versions);
    }

    public Biome getBiome() {
        try {
            for (Version version : this.versions) {
                if (ServerVersion.isServerVersionAtLeast(version.version)) {
                    return Biome.valueOf(version.biome);
                }
            }
        } catch (IllegalArgumentException ignore) { // This means the supporter biome server version is wrongly configured
        }

        return null;
    }

    public static Set<CompatibleBiome> getCompatibleBiomes() {
        return compatibleBiomes;
    }

    public static CompatibleBiome getBiome(Biome biome) {
        return biome == null ? null : lookupMap.get(biome.name());
    }

    public static CompatibleBiome getBiome(String name) {
        return name == null ? null : lookupMap.get(name.toUpperCase());
    }

    public void setBiome(Chunk chunk) throws InvocationTargetException, IllegalAccessException {
        Object nmsChunk = null;
        Object biomeStorage = null;
        Object biomeBase = null;

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15) && ServerVersion.isServerVersionBelow(ServerVersion.V1_18)) {
            nmsChunk = methodGetHandle.invoke(chunk);
            biomeStorage = methodGetBiomeIndex.invoke(nmsChunk);

            if (isAbove1_16_R1) {
                Object registry = fieldStorageRegistry.get(biomeStorage);
                biomeBase = methodBiomeToBiomeBase.invoke(null, registry, getBiome());
            } else {
                biomeBase = methodBiomeToBiomeBase.invoke(null, getBiome());
            }
        }

        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                for (int y = 0; y < world.getMaxHeight(); ++y) {
                    if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_18)) {
                        chunk.getWorld().setBiome(x, y, z, getBiome());
                    } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15) && ServerVersion.isServerVersionBelow(ServerVersion.V1_18)) {
                        methodSetBiome.invoke(biomeStorage, x >> 2, y >> 2, z >> 2, biomeBase);
                    }
                }

                if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15)) {
                    continue;
                }

                chunk.getWorld().setBiome(x, z, getBiome());
            }
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15) && ServerVersion.isServerVersionBelow(ServerVersion.V1_18)) {
            methodMarkDirty.invoke(nmsChunk);
        }
    }

    public void setBiome(World world, int x, int y, int z) {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_15)) {
            world.setBiome(x, y, z, getBiome());

            return;
        }

        world.setBiome(x, z, getBiome());
    }

    private static Version v(ServerVersion version, String biome) {
        return new Version(version, biome);
    }

    private static Version v(String biome) {
        return new Version(ServerVersion.UNKNOWN, biome);
    }

    private static class Version {
        final ServerVersion version;
        final String biome;

        public Version(ServerVersion version, String biome) {
            this.version = version;
            this.biome = biome;
        }
    }
}
