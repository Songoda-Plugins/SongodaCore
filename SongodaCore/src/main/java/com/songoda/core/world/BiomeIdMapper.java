package com.songoda.core.world;

import com.songoda.core.compatibility.KnownServerVersions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiomeIdMapper {

    private final Map<String, Integer> activeBiomeMap = new HashMap<>();

    public BiomeIdMapper(KnownServerVersions version) {
        // 1. The baseline 1.18.2 ordered registry
        List<String> registryList = new ArrayList<>(Arrays.asList(
                "minecraft:the_void",                  // 0
                "minecraft:plains",                    // 1
                "minecraft:sunflower_plains",          // 2
                "minecraft:snowy_plains",              // 3
                "minecraft:ice_spikes",                // 4
                "minecraft:desert",                    // 5
                "minecraft:swamp",                     // 6
                "minecraft:forest",                    // 7 (shifts in 1.19)
                "minecraft:flower_forest",             // 8
                "minecraft:birch_forest",              // 9
                "minecraft:dark_forest",               // 10
                "minecraft:old_growth_birch_forest",   // 11
                "minecraft:old_growth_pine_taiga",     // 12
                "minecraft:old_growth_spruce_taiga",   // 13
                "minecraft:taiga",                     // 14
                "minecraft:snowy_taiga",               // 15
                "minecraft:savanna",                   // 16
                "minecraft:savanna_plateau",           // 17
                "minecraft:windswept_hills",           // 18
                "minecraft:windswept_gravelly_hills",  // 19
                "minecraft:windswept_forest",          // 20
                "minecraft:windswept_savanna",         // 21
                "minecraft:jungle",                    // 22
                "minecraft:sparse_jungle",             // 23
                "minecraft:bamboo_jungle",             // 24
                "minecraft:badlands",                  // 25
                "minecraft:eroded_badlands",           // 26
                "minecraft:wooded_badlands",           // 27
                "minecraft:meadow",                    // 28
                "minecraft:grove",                     // 29 (shifts in 1.20)
                "minecraft:snowy_slopes",              // 30
                "minecraft:frozen_peaks",              // 31
                "minecraft:jagged_peaks",              // 32
                "minecraft:stony_peaks",               // 33
                "minecraft:river",                     // 34
                "minecraft:frozen_river",              // 35
                "minecraft:beach",                     // 36
                "minecraft:snowy_beach",               // 37
                "minecraft:stony_shore",               // 38
                "minecraft:warm_ocean",                // 39
                "minecraft:lukewarm_ocean",            // 40
                "minecraft:deep_lukewarm_ocean",       // 41
                "minecraft:ocean",                     // 42
                "minecraft:deep_ocean",                // 43
                "minecraft:cold_ocean",                // 44
                "minecraft:deep_cold_ocean",           // 45
                "minecraft:frozen_ocean",              // 46
                "minecraft:deep_frozen_ocean",         // 47
                "minecraft:mushroom_fields",           // 48
                "minecraft:dripstone_caves",           // 49
                "minecraft:lush_caves",                // 50
                "minecraft:nether_wastes",             // 51 (shifts in 1.19)
                "minecraft:warped_forest",             // 52
                "minecraft:crimson_forest",            // 53
                "minecraft:soul_sand_valley",          // 54
                "minecraft:basalt_deltas",             // 55
                "minecraft:the_end",                   // 56
                "minecraft:end_highlands",             // 57
                "minecraft:end_midlands",              // 58
                "minecraft:small_end_islands",         // 59
                "minecraft:end_barrens"                // 60
        ));

        // 2. Apply 1.19.x insertions
        // Using ordinal() is safe here because your enum is strictly chronological
        if (version.ordinal() >= KnownServerVersions.V1_19.ordinal()) {
            registryList.add(7, "minecraft:mangrove_swamp");
            registryList.add(52, "minecraft:deep_dark");
        }

        // 3. Apply 1.20.x insertions
        if (version.ordinal() >= KnownServerVersions.V1_20.ordinal()) {
            registryList.add(30, "minecraft:cherry_grove");
        }

        // 4. Bake the final list into a HashMap for O(1) lookups during packets
        for (int i = 0; i < registryList.size(); i++) {
            activeBiomeMap.put(registryList.get(i), i);
        }
    }

    /**
     * Gets the biome ID for the initialized server version.
     */
    public int getId(String biomeKey) {
        return activeBiomeMap.getOrDefault(biomeKey, 1); // 1 = plains fallback
    }
}