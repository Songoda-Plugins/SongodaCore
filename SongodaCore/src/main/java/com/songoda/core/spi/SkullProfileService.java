package com.songoda.core.spi;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;

/**
 * Paper-compatible player-profile operations on {@link SkullMeta}.
 */
public interface SkullProfileService {

    void applyTexture(ItemStack head, String textureUrl);

    void applyTexture(ItemStack head, String textureUrl, UUID profileId);

    void applyTexture(ItemMeta meta, String textureUrl);

    void applyTexture(ItemMeta meta, String textureUrl, UUID profileId);

    void serializeSkull(SkullMeta skullMeta, Map<String, Object> map);

    void deserializeSkull(SkullMeta skullMeta, Map<String, Object> map);
}
