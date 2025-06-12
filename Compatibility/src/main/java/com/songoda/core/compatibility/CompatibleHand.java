package com.songoda.core.compatibility;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Get which hand is being used.
 */
public enum CompatibleHand {
    MAIN_HAND, OFF_HAND;

    private static final Map<String, Method> methodCache = new HashMap<>();

    public static CompatibleHand getHand(Object event) {
        try {
            Class<?> clazz = event.getClass();
            String className = clazz.getName();

            Method method;
            if (methodCache.containsKey(className)) {
                method = methodCache.get(className);
            } else {
                method = clazz.getDeclaredMethod("getHand");
                methodCache.put(className, method);
            }

            EquipmentSlot slot = (EquipmentSlot) method.invoke(event);

            if (slot == EquipmentSlot.OFF_HAND) {
                return OFF_HAND;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
        }

        return MAIN_HAND;
    }

    public static CompatibleHand getHand(EquipmentSlot equipmentSlot) {
        return equipmentSlot == EquipmentSlot.HAND ? MAIN_HAND : OFF_HAND;
    }

    /**
     * Use up whatever item the player is holding in their hand
     *
     * @param entity entity to grab item from
     */
    public void takeItem(LivingEntity entity) {
        takeItem(entity, 1);
    }

    /**
     * Use up whatever item the player is holding in their hand
     *
     * @param entity entity to grab item from
     * @param amount number of items to use up
     */
    public void takeItem(LivingEntity entity, int amount) {
        ItemStack item = this == CompatibleHand.MAIN_HAND
                ? entity.getEquipment().getItemInHand() : entity.getEquipment().getItemInOffHand();

        int result = item.getAmount() - amount;
        item.setAmount(result);

        if (this == CompatibleHand.MAIN_HAND) {
            entity.getEquipment().setItemInHand(result > 0 ? item : null);
            return;
        }

        entity.getEquipment().setItemInOffHand(result > 0 ? item : null);
    }

    /**
     * Get item in the selected hand
     *
     * @param entity The entity to get the item from
     *
     * @return The item or null
     */
    public ItemStack getItem(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }

        //getItemInMainHand doesn't exist in 1.8
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
            return equipment.getItemInHand();
        }

        if (this == MAIN_HAND) {
            return equipment.getItemInMainHand();
        }
        return equipment.getItemInOffHand();
    }

    /**
     * Set the item in the selected hand
     *
     * @param entity The entity to set the item of
     * @param item   The item to set
     */
    public void setItem(LivingEntity entity, ItemStack item) {
        if (this == MAIN_HAND) {
            entity.getEquipment().setItemInHand(item);
            return;
        }

        entity.getEquipment().setItemInOffHand(item);
    }
}
