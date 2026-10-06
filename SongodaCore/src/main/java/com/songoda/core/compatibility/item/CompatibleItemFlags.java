package com.songoda.core.compatibility.item;

import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum CompatibleItemFlags {

    HIDE_ENCHANTS(KnownServerVersions.V1_18),
    HIDE_ATTRIBUTES(KnownServerVersions.V1_18),
    HIDE_UNBREAKABLE(KnownServerVersions.V1_18),
    HIDE_DESTROYS(KnownServerVersions.V1_18),
    HIDE_PLACED_ON(KnownServerVersions.V1_18),
    HIDE_POTION_EFFECTS(KnownServerVersions.V1_18),
    HIDE_DYE(KnownServerVersions.V1_18),

    // 1.18.2+ flags
    HIDE_ARMOR_TRIM(KnownServerVersions.V1_20),
    HIDE_ADDITIONAL_TOOLTIP(KnownServerVersions.V1_20_5),
    HIDE_STORED_ENCHANTS(KnownServerVersions.V1_20_5);

    private final KnownServerVersions minimumVersion;

    CompatibleItemFlags(KnownServerVersions minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public static void applyAll(ItemMeta meta, CompatibleItemFlags... flags) {
        for (CompatibleItemFlags flag : flags) {
            flag.apply(meta);
        }
    }

    public static void applyAll(ItemStack item, CompatibleItemFlags... flags) {
        for (CompatibleItemFlags flag : flags) {
            flag.apply(item);
        }
    }

    public boolean isSupported() {
        return ServerVersion.isAtLeastVersion(minimumVersion);
    }

    public void apply(ItemMeta meta) {
        if (!isSupported()) return;
        meta.addItemFlags(ItemFlag.valueOf(this.name()));
    }

    public void apply(ItemStack item) {
        if (!isSupported()) return;
        item.addItemFlags(ItemFlag.valueOf(this.name()));
    }
}
