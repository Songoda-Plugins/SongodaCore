package com.songoda.core.utils;

import com.songoda.core.spi.SkullProfiles;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Custom skull textures. Delegates to {@link com.songoda.core.spi.SkullProfiles}: Paper uses
 * {@code PlayerProfile} / {@code PlayerTextures} on Paper and supported Paper forks.
 */
public class HeadUtils {

    public static void applyTexture(ItemStack head, String textureUrl) {
        SkullProfiles.get().applyTexture(head, textureUrl);
    }

    public static void applyTexture(ItemStack head, String textureUrl, UUID profileId) {
        SkullProfiles.get().applyTexture(head, textureUrl, profileId);
    }

    public static void applyTexture(ItemMeta meta, String textureUrl) {
        SkullProfiles.get().applyTexture(meta, textureUrl);
    }

    public static void applyTexture(ItemMeta meta, String textureUrl, UUID profileId) {
        SkullProfiles.get().applyTexture(meta, textureUrl, profileId);
    }
}
