package com.songoda.core.compatibility.crops;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompatibleCrop {
    private static final boolean USE_LEGACY_IMPLEMENTATION;

    static {
        boolean useLegacy = false;
        try {
            Class.forName("org.bukkit.block.data.Ageable");
        } catch (ClassNotFoundException ignore) {
            useLegacy = true;
        }
        USE_LEGACY_IMPLEMENTATION = useLegacy;
    }

    public static boolean isCrop(@Nullable Block block) {
        if (block == null) {
            return false;
        }

        XMaterial material = CompatibleMaterial.getMaterial(block.getType()).get();
        return isCrop(material);
    }

    public static boolean isCrop(@Nullable XMaterial material) {
        if (material == XMaterial.SWEET_BERRY_BUSH) {
            return true;
        }
        return material != null && XBlock.isCrop(material);
    }

    public static boolean isCropFullyGrown(@NotNull Block crop) {
        return getCropAge(crop) >= getCropMaxAge(crop);
    }

    public static int getCropAge(@NotNull Block crop) {
        if (!USE_LEGACY_IMPLEMENTATION) {
            return CompatibleCropModern.getCropAge(crop);
        }
        return crop.getData();
    }

    public static int getCropMaxAge(@NotNull Block crop) {
        if (!USE_LEGACY_IMPLEMENTATION) {
            return CompatibleCropModern.getCropMaxAge(crop);
        }

        switch (CompatibleMaterial.getMaterial(crop.getType()).get()) {
            case BEETROOTS:
            case NETHER_WART:
            case SWEET_BERRY_BUSH:
                return 3;
            default:
                return 7;
        }
    }

    public static void resetCropAge(@NotNull Block crop) {
        setCropAge(crop, 0);
    }

    public static void incrementCropAge(@NotNull Block crop) {
        setCropAge(crop, getCropAge(crop) + 1);
    }

    private static void setCropAge(Block block, int stage) {
        if (stage > getCropMaxAge(block)) {
            return;
        }

        if (!USE_LEGACY_IMPLEMENTATION) {
            CompatibleCropModern.setGrowthStage(block, stage);
            return;
        }

        try {
            Block.class.getDeclaredMethod("setData", byte.class).invoke(block, (byte) stage);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
