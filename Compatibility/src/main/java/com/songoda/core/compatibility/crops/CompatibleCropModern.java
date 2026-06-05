package com.songoda.core.compatibility.crops;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

class CompatibleCropModern {
    static int getCropAge(Block block) {
        BlockData blockData = block.getBlockData();
        return ((Ageable) blockData).getAge();
    }

    static int getCropMaxAge(Block block) {
        BlockData blockData = block.getBlockData();
        return ((Ageable) blockData).getMaximumAge();
    }

    static void setGrowthStage(Block block, int stage) {
        Ageable blockData = (Ageable) block.getBlockData();
        blockData.setAge(stage);
        block.setBlockData(blockData);
    }
}
