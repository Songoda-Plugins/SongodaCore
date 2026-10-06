package com.songoda.core.vinject.serializer;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ItemStackSerializerAttributeTest {

    @Test
    public void testResolveOperation() throws Exception {
        Method resolveMethod = ItemStackSerializer.class.getDeclaredMethod("resolveOperation", String.class);
        resolveMethod.setAccessible(true);

        assertEquals(AttributeModifier.Operation.ADD_NUMBER, resolveMethod.invoke(null, "ADD_NUMBER"));
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, resolveMethod.invoke(null, "add_number"));
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, resolveMethod.invoke(null, "ADD"));
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, resolveMethod.invoke(null, "0"));

        assertEquals(AttributeModifier.Operation.ADD_SCALAR, resolveMethod.invoke(null, "ADD_SCALAR"));
        assertEquals(AttributeModifier.Operation.ADD_SCALAR, resolveMethod.invoke(null, "1"));

        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, resolveMethod.invoke(null, "MULTIPLY_SCALAR_1"));
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, resolveMethod.invoke(null, "MULTIPLY"));
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, resolveMethod.invoke(null, "2"));
    }

    @Test
    public void testResolveSlotGroup() throws Exception {
        assertEquals(EquipmentSlotGroup.HEAD, ModernPaperItemApi.resolveSlotGroup("HEAD"));
        assertEquals(EquipmentSlotGroup.HEAD, ModernPaperItemApi.resolveSlotGroup("head"));
        assertEquals(EquipmentSlotGroup.CHEST, ModernPaperItemApi.resolveSlotGroup("CHEST"));
        assertEquals(EquipmentSlotGroup.ARMOR, ModernPaperItemApi.resolveSlotGroup("ARMOR"));
        assertEquals(EquipmentSlotGroup.ANY, ModernPaperItemApi.resolveSlotGroup(""));
        assertEquals(EquipmentSlotGroup.ANY, ModernPaperItemApi.resolveSlotGroup(null));
    }

    @Test
    public void testResolveAttributeNullOrBlank() throws Exception {
        Method resolveMethod = ItemStackSerializer.class.getDeclaredMethod("resolveAttribute", String.class);
        resolveMethod.setAccessible(true);

        assertNull(resolveMethod.invoke(null, (String) null));
        assertNull(resolveMethod.invoke(null, ""));
        assertNull(resolveMethod.invoke(null, "   "));
        assertNull(resolveMethod.invoke(null, "unknown_attribute_xyz"));
    }
}
