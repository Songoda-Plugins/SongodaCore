package com.songoda.core.vinject.serializer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/**
 * Newer Paper item APIs kept separate so older servers only load this class behind a version check.
 */
final class ModernPaperItemApi {

    private ModernPaperItemApi() {
    }

    static String readSlotGroup(AttributeModifier modifier) {
        EquipmentSlotGroup slotGroup = modifier.getSlotGroup();
        return slotGroup == EquipmentSlotGroup.ANY ? null : slotGroup.toString().toUpperCase(Locale.ROOT);
    }

    static AttributeModifier createAttributeModifier(NamespacedKey key, double amount,
                                                     AttributeModifier.Operation operation, String slotName) {
        return new AttributeModifier(key, amount, operation, resolveSlotGroup(slotName));
    }

    static EquipmentSlotGroup resolveSlotGroup(String slotName) {
        if (slotName == null || slotName.isBlank()) {
            return EquipmentSlotGroup.ANY;
        }
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.getByName(slotName.trim().toLowerCase(Locale.ROOT));
        return slotGroup == null ? EquipmentSlotGroup.ANY : slotGroup;
    }

    static boolean hasItemModel(ItemMeta meta) {
        return meta.hasItemModel();
    }

    static NamespacedKey readItemModel(ItemMeta meta) {
        return meta.getItemModel();
    }

    static void writeItemModel(ItemMeta meta, NamespacedKey model) {
        meta.setItemModel(model);
    }

    static Key readTooltipStyle(ItemStack item) {
        return item.getData(DataComponentTypes.TOOLTIP_STYLE);
    }

    static void writeTooltipStyle(ItemStack item, String style) {
        item.setData(DataComponentTypes.TOOLTIP_STYLE, Key.key(style));
    }
}
