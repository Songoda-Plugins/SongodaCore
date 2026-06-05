package com.songoda.core.utils;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EntityUtils {
    // TODO: #getSpawnBlocks should probably be moved into the Compatibility sub-module
    public static List<XMaterial> getSpawnBlocks(EntityType type) {
        switch (type.name()) {
            case "PIG":
            case "SHEEP":
            case "CHICKEN":
            case "COW":
            case "RABBIT":
            case "LLAMA":
            case "HORSE":
            case "CAT":
                return Collections.singletonList(XMaterial.GRASS_BLOCK);

            case "MUSHROOM_COW":
                return Collections.singletonList(XMaterial.MYCELIUM);

            case "SQUID":
            case "ELDER_GUARDIAN":
            case "COD":
            case "SALMON":
            case "PUFFERFISH":
            case "TROPICAL_FISH":
                return Collections.singletonList(XMaterial.WATER);

            case "OCELOT":
                return Arrays.asList(XMaterial.GRASS_BLOCK,
                        XMaterial.JUNGLE_LEAVES, XMaterial.ACACIA_LEAVES,
                        XMaterial.BIRCH_LEAVES, XMaterial.DARK_OAK_LEAVES,
                        XMaterial.OAK_LEAVES, XMaterial.SPRUCE_LEAVES);

            default:
                return Collections.singletonList(XMaterial.AIR);
        }
    }
}
