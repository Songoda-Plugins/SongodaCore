package com.craftaro.core.compatibility;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class CompatibleMaterial {
    private static final Map<XMaterial, ItemStack> FURNACE_RESULT_CACHE = new HashMap<>();

    public static Optional<XMaterial> getMaterial(@NotNull Material material) {
        return getMaterial(material.name());
    }

    public static Optional<XMaterial> getMaterial(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        return XMaterial.matchXMaterial(name);
    }

    public static Optional<XMaterial> getMaterialForUserInput(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        return getMaterial(name.toUpperCase().replace(" ", "_"));
    }

    public static boolean isAir(@NotNull XMaterial material) {
        return material == XMaterial.AIR ||
                material == XMaterial.CAVE_AIR ||
                material == XMaterial.VOID_AIR;
    }

    public static Optional<XMaterial> getSpawnEgg(EntityType type) {
        if (type == EntityType.MUSHROOM_COW) {
            return Optional.of(XMaterial.MOOSHROOM_SPAWN_EGG);
        }
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_16) && type == EntityType.valueOf("PIG_ZOMBIE")) {
            return Optional.of(XMaterial.ZOMBIFIED_PIGLIN_SPAWN_EGG);
        }

        return getMaterial(type.name() + "_SPAWN_EGG");
    }

    public static EntityType getEntityForSpawnEgg(XMaterial material) {
        if (!material.name().endsWith("_SPAWN_EGG")) {
            throw new IllegalArgumentException("Material is not a spawn egg");
        }

        String entityName = material.name().substring(0, material.name().length() - "_SPAWN_EGG".length());

        if (entityName.equals("MOOSHROOM")) {
            entityName = "MUSHROOM_COW";
        } else if (entityName.equals("ZOMBIE_PIGMAN")) {
            entityName = "PIG_ZOMBIE";
        }

        try {
            return EntityType.valueOf(entityName);
        } catch (IllegalArgumentException ignore) {
        }
        return null;
    }

    public static @Nullable XMaterial getYieldForCrop(XMaterial material) {
        switch (material) {
            case BEETROOTS:
                return XMaterial.BEETROOT;
            case CACTUS:
                return XMaterial.CACTUS;
            case CARROTS:
                return XMaterial.CARROT;
            case CHORUS_FLOWER:
                return XMaterial.CHORUS_PLANT;
            case KELP:
                return XMaterial.KELP;
            case MELON_STEM:
                return XMaterial.MELON;
            case NETHER_WART:
                return XMaterial.NETHER_WART;
            case POTATOES:
                return XMaterial.POTATO;
            case PUMPKIN_STEM:
                return XMaterial.PUMPKIN;
            case SUGAR_CANE:
                return XMaterial.SUGAR_CANE;
            case WHEAT:
                return XMaterial.WHEAT;
            case BAMBOO:
                return XMaterial.BAMBOO;
            default:
                return null;
        }
    }

    public static @Nullable XMaterial getSeedForCrop(XMaterial material) {
        switch (material) {
            case BEETROOTS:
                return XMaterial.BEETROOT_SEEDS;
            case CACTUS:
                return XMaterial.CACTUS;
            case CARROTS:
                return XMaterial.CARROT;
            case CHORUS_PLANT:
                return XMaterial.CHORUS_FLOWER;
            case KELP:
                return XMaterial.KELP;
            case MELON_STEM:
                return XMaterial.MELON_SEEDS;
            case NETHER_WART:
                return XMaterial.NETHER_WART;
            case POTATOES:
                return XMaterial.POTATO;
            case PUMPKIN_STEM:
                return XMaterial.PUMPKIN_SEEDS;
            case SUGAR_CANE:
                return XMaterial.SUGAR_CANE;
            case WHEAT:
                return XMaterial.WHEAT_SEEDS;
            case BAMBOO:
                return XMaterial.BAMBOO;
            default:
                return null;
        }
    }

    /**
     * TODO: Check if used by ItemUtils when ready for Core v3 and if yes maybe re-implement to not need manual updating
     */
    @Deprecated
    public static boolean isFurnaceFuel(XMaterial material) {
        // this function is not implemented in some older versions, so we need this here...
        switch (material) {
            case ACACIA_BOAT:
            case ACACIA_BUTTON:
            case ACACIA_DOOR:
            case ACACIA_FENCE:
            case ACACIA_FENCE_GATE:
            case ACACIA_LOG:
            case ACACIA_PLANKS:
            case ACACIA_PRESSURE_PLATE:
            case ACACIA_SAPLING:
            case ACACIA_SIGN:
            case ACACIA_SLAB:
            case ACACIA_STAIRS:
            case ACACIA_TRAPDOOR:
            case ACACIA_WOOD:
            case AZALEA:
            case BAMBOO:
            case BARREL:
            case BIRCH_BOAT:
            case BIRCH_BUTTON:
            case BIRCH_DOOR:
            case BIRCH_FENCE:
            case BIRCH_FENCE_GATE:
            case BIRCH_LOG:
            case BIRCH_PLANKS:
            case BIRCH_PRESSURE_PLATE:
            case BIRCH_SAPLING:
            case BIRCH_SIGN:
            case BIRCH_SLAB:
            case BIRCH_STAIRS:
            case BIRCH_TRAPDOOR:
            case BIRCH_WOOD:
            case BLACK_BANNER:
            case BLACK_CARPET:
            case BLACK_WOOL:
            case BLAZE_ROD:
            case BLUE_BANNER:
            case BLUE_CARPET:
            case BLUE_WOOL:
            case BOOKSHELF:
            case BOW:
            case BOWL:
            case BROWN_BANNER:
            case BROWN_CARPET:
            case BROWN_WOOL:
            case CARTOGRAPHY_TABLE:
            case CHARCOAL:
            case CHEST:
            case COAL:
            case COAL_BLOCK:
            case COMPOSTER:
            case CRAFTING_TABLE:
            case CROSSBOW:
            case CYAN_BANNER:
            case CYAN_CARPET:
            case CYAN_WOOL:
            case DARK_OAK_BOAT:
            case DARK_OAK_BUTTON:
            case DARK_OAK_DOOR:
            case DARK_OAK_FENCE:
            case DARK_OAK_FENCE_GATE:
            case DARK_OAK_LOG:
            case DARK_OAK_PLANKS:
            case DARK_OAK_PRESSURE_PLATE:
            case DARK_OAK_SAPLING:
            case DARK_OAK_SIGN:
            case DARK_OAK_SLAB:
            case DARK_OAK_STAIRS:
            case DARK_OAK_TRAPDOOR:
            case DARK_OAK_WOOD:
            case DAYLIGHT_DETECTOR:
            case DEAD_BUSH:
            case DRIED_KELP_BLOCK:
            case FISHING_ROD:
            case FLETCHING_TABLE:
            case FLOWERING_AZALEA:
            case GRAY_BANNER:
            case GRAY_CARPET:
            case GRAY_WOOL:
            case GREEN_BANNER:
            case GREEN_CARPET:
            case GREEN_WOOL:
            case JUKEBOX:
            case JUNGLE_BOAT:
            case JUNGLE_BUTTON:
            case JUNGLE_DOOR:
            case JUNGLE_FENCE:
            case JUNGLE_FENCE_GATE:
            case JUNGLE_LOG:
            case JUNGLE_PLANKS:
            case JUNGLE_PRESSURE_PLATE:
            case JUNGLE_SAPLING:
            case JUNGLE_SIGN:
            case JUNGLE_SLAB:
            case JUNGLE_STAIRS:
            case JUNGLE_TRAPDOOR:
            case JUNGLE_WOOD:
            case LADDER:
            case LAVA_BUCKET:
            case LECTERN:
            case LIGHT_BLUE_BANNER:
            case LIGHT_BLUE_CARPET:
            case LIGHT_BLUE_WOOL:
            case LIGHT_GRAY_BANNER:
            case LIGHT_GRAY_CARPET:
            case LIGHT_GRAY_WOOL:
            case LIME_BANNER:
            case LIME_CARPET:
            case LIME_WOOL:
            case LOOM:
            case MAGENTA_BANNER:
            case MAGENTA_CARPET:
            case MAGENTA_WOOL:
            case NOTE_BLOCK:
            case OAK_BOAT:
            case OAK_BUTTON:
            case OAK_DOOR:
            case OAK_FENCE:
            case OAK_FENCE_GATE:
            case OAK_LOG:
            case OAK_PLANKS:
            case OAK_PRESSURE_PLATE:
            case OAK_SAPLING:
            case OAK_SIGN:
            case OAK_SLAB:
            case OAK_STAIRS:
            case OAK_TRAPDOOR:
            case OAK_WOOD:
            case ORANGE_BANNER:
            case ORANGE_CARPET:
            case ORANGE_WOOL:
            case PINK_BANNER:
            case PINK_CARPET:
            case PINK_WOOL:
            case PURPLE_BANNER:
            case PURPLE_CARPET:
            case PURPLE_WOOL:
            case RED_BANNER:
            case RED_CARPET:
            case RED_WOOL:
            case SCAFFOLDING:
            case SMITHING_TABLE:
            case SPRUCE_BOAT:
            case SPRUCE_BUTTON:
            case SPRUCE_DOOR:
            case SPRUCE_FENCE:
            case SPRUCE_FENCE_GATE:
            case SPRUCE_LOG:
            case SPRUCE_PLANKS:
            case SPRUCE_PRESSURE_PLATE:
            case SPRUCE_SAPLING:
            case SPRUCE_SIGN:
            case SPRUCE_SLAB:
            case SPRUCE_STAIRS:
            case SPRUCE_TRAPDOOR:
            case SPRUCE_WOOD:
            case STICK:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_ACACIA_WOOD:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_BIRCH_WOOD:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_DARK_OAK_WOOD:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_JUNGLE_WOOD:
            case STRIPPED_OAK_LOG:
            case STRIPPED_OAK_WOOD:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_SPRUCE_WOOD:
            case TRAPPED_CHEST:
            case WHITE_BANNER:
            case WHITE_CARPET:
            case WHITE_WOOL:
            case WOODEN_AXE:
            case WOODEN_HOE:
            case WOODEN_PICKAXE:
            case WOODEN_SHOVEL:
            case WOODEN_SWORD:
            case YELLOW_BANNER:
            case YELLOW_CARPET:
            case YELLOW_WOOL:
                return true;
            default:
                return false;
        }
    }

    public static @Nullable ItemStack getFurnaceResult(XMaterial material) {
        if (FURNACE_RESULT_CACHE.isEmpty()) {
            Iterator<Recipe> recipes = Bukkit.recipeIterator();
            while (recipes.hasNext()) {
                Recipe recipe = recipes.next();
                if (!(recipe instanceof FurnaceRecipe)) {
                    continue;
                }

                FURNACE_RESULT_CACHE.put(XMaterial.matchXMaterial(((FurnaceRecipe) recipe).getInput()), recipe.getResult());
            }
        }

        ItemStack furnaceResult = FURNACE_RESULT_CACHE.get(material);
        if (furnaceResult != null) {
            return furnaceResult.clone();
        }
        return null;
    }

    public static boolean isBrewingStandIngredient(XMaterial material) {
        switch (material) {
            case NETHER_WART:
            case REDSTONE:
            case GLOWSTONE_DUST:
            case FERMENTED_SPIDER_EYE:
            case GUNPOWDER:
            case DRAGON_BREATH:

            case SUGAR:
            case RABBIT_FOOT:
            case GLISTERING_MELON_SLICE:
            case SPIDER_EYE:
            case PUFFERFISH:
            case MAGMA_CREAM:
            case GOLDEN_CARROT:
            case BLAZE_POWDER:
            case GHAST_TEAR:
            case TURTLE_HELMET:
            case PHANTOM_MEMBRANE:
                return true;

            default:
                return false;
        }
    }

    /**
     * TODO: Check if usages on this can be removed otherwise remove deprecation annotation
     */
    @Deprecated
    public static XMaterial getGlassPaneForColor(int color) {
        switch (color) {
            case 0:
                return XMaterial.WHITE_STAINED_GLASS_PANE;
            case 1:
                return XMaterial.ORANGE_STAINED_GLASS_PANE;
            case 2:
                return XMaterial.MAGENTA_STAINED_GLASS_PANE;
            case 3:
                return XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 4:
                return XMaterial.YELLOW_STAINED_GLASS_PANE;
            case 5:
                return XMaterial.LIME_STAINED_GLASS_PANE;
            case 6:
                return XMaterial.PINK_STAINED_GLASS_PANE;
            case 7:
                return XMaterial.GRAY_STAINED_GLASS_PANE;
            case 8:
                return XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 9:
                return XMaterial.CYAN_STAINED_GLASS_PANE;
            case 10:
                return XMaterial.PURPLE_STAINED_GLASS_PANE;
            case 11:
                return XMaterial.BLUE_STAINED_GLASS_PANE;
            case 12:
                return XMaterial.BROWN_STAINED_GLASS_PANE;
            case 13:
                return XMaterial.GREEN_STAINED_GLASS_PANE;
            case 14:
                return XMaterial.RED_STAINED_GLASS_PANE;
            case 15:
                return XMaterial.BLACK_STAINED_GLASS_PANE;
            default:
                return XMaterial.WHITE_STAINED_GLASS;
        }
    }
}
