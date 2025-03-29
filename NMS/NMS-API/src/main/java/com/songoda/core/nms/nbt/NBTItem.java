package com.songoda.core.nms.nbt;

import org.bukkit.inventory.ItemStack;

/**
 * @deprecated Use {@link de.tr7zw.nbtapi.NBTItem} instead.
 */
@Deprecated
public interface NBTItem extends NBTCompound {
    ItemStack finish();
}
