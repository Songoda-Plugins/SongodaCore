package com.songoda.core.nms.world;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public interface SWorld {
    List<LivingEntity> getLivingEntities();

    /**
     * Set a block to a certain type by updating the block directly in the NMS chunk.
     * <br>
     * The chunk must be loaded and players must relog if they have the chunk loaded in order to use this method.
     * (F3+A is not enough)
     */
    // TODO: Check if FabledSkyBlock *really* needs this method and if it can be removed.
    //       Would make thinks less complicated and I kinda cannot imagine it being *that* much faster to be worth it?
    void setBlockFast(int x, int y, int z, Material material);
}
