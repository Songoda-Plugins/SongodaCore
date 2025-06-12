package com.songoda.core.compatibility;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Starting in Minecraft 1.13, separate materials for blocks and items were
 * phased out. This provides a translation for those values.
 */
public enum LegacyMaterialBlockType {
    OAK_LEAVES("LEAVES", (byte) 8, true),
    SPRUCE_LEAVES("LEAVES", (byte) 9, true),
    BIRCH_LEAVES("LEAVES", (byte) 10, true),
    JUNGLE_LEAVES("LEAVES", (byte) 11, true),
    ACACIA_LEAVES("LEAVES_2", (byte) 8, true),
    DARK_OAK_LEAVES("LEAVES_2", (byte) 9, true),

    ACACIA_DOOR("ACACIA_DOOR", true),
    BED("BED_BLOCK", true),
    BIRCH_DOOR("BIRCH_DOOR", true),
    FURNACE("FURNACE", "BURNING_FURNACE"),
    CAKE("CAKE_BLOCK"),
    CARROTS("CARROT"), // totally makes sense, lol
    CAULDRON("CAULDRON_BLOCK"),
    COMPARATOR("REDSTONE_COMPARATOR_OFF", "REDSTONE_COMPARATOR_ON"),
    DARK_OAK_DOOR("DARK_OAK_DOOR", true),
    DAYLIGHT_DETECTOR("DAYLIGHT_DETECTOR", "DAYLIGHT_DETECTOR_INVERTED"),
    /*
     <     DOUBLE_STEP,
     <     DOUBLE_STONE_SLAB2,
     */
    FLOWER_POT("FLOWER_POT"),
    IRON_DOOR("IRON_DOOR_BLOCK", true),
    JUNGLE_DOOR("JUNGLE_DOOR", true),
    LAVA("STATIONARY_LAVA"),
    NETHER_WART("NETHER_WARTS"),
    /*
     <     PURPUR_DOUBLE_SLAB
     */
    POTATOES("POTATO"),
    REDSTONE_LAMP("REDSTONE_LAMP_OFF", "REDSTONE_LAMP_ON"),
    REDSTONE_ORE("REDSTONE_ORE", "GLOWING_REDSTONE_ORE"),
    REDSTONE_TORCH("REDSTONE_TORCH_ON", "REDSTONE_TORCH_OFF"),
    SPRUCE_DOOR("SPRUCE_DOOR"),
    SUGAR_CANE("SUGAR_CANE_BLOCK"),
    WATER("STATIONARY_WATER"),
    WHEAT("CROPS");
    final String blockMaterialName;
    final byte blockData;
    final String alternateBlockMaterialName;
    final Material blockMaterial, alternateBlockMaterial;
    final boolean requiresData; // some blocks require data to render properly (double blocks)
    static final Map<String, LegacyMaterialBlockType> lookupTable = new HashMap<>();
    static final Map<String, LegacyMaterialBlockType> reverseLookupTable = new HashMap<>();

    static {
        for (LegacyMaterialBlockType t : values()) {
            lookupTable.put(t.name(), t);
            reverseLookupTable.put(t.blockMaterialName + ":" + t.blockData, t);
            if (t.alternateBlockMaterialName != null) {
                reverseLookupTable.put(t.alternateBlockMaterialName, t);
            }
        }
    }

    LegacyMaterialBlockType(String blockMaterial) {
        this(blockMaterial, (byte) -1, null, false);
    }

    LegacyMaterialBlockType(String blockMaterial, boolean requiresData) {
        this(blockMaterial, (byte) -1, null, requiresData);
    }

    LegacyMaterialBlockType(String blockMaterial, byte data, boolean requiresData) {
        this(blockMaterial, data, null, requiresData);
    }

    LegacyMaterialBlockType(String blockMaterial, String alternateMaterial) {
        this(blockMaterial, (byte) -1, alternateMaterial, false);
    }

    LegacyMaterialBlockType(String blockMaterial, byte data, String alternateMaterial, boolean requiresData) {
        this.blockMaterialName = blockMaterial;
        this.alternateBlockMaterialName = alternateMaterial;
        this.requiresData = requiresData;
        this.blockMaterial = Material.getMaterial(this.blockMaterialName);
        this.alternateBlockMaterial = Material.getMaterial(this.alternateBlockMaterialName);
        this.blockData = data;
    }

    public String getBlockMaterialName() {
        return this.blockMaterialName;
    }

    public String getAlternateMaterialName() {
        return this.alternateBlockMaterialName;
    }

    public Material getBlockMaterial() {
        return this.blockMaterial;
    }

    public Material getAlternateBlockMaterial() {
        return this.alternateBlockMaterial;
    }

    public boolean requiresData() {
        return this.requiresData;
    }

    public static LegacyMaterialBlockType getMaterial(String lookup) {
        return lookupTable.get(lookup);
    }

    public static LegacyMaterialBlockType getFromLegacy(String lookup, byte data) {
        return reverseLookupTable.get(lookup + ":" + data);
    }

    public static LegacyMaterialBlockType getFromLegacy(String lookup) {
        return getFromLegacy(lookup, (byte) -1);
    }
}
