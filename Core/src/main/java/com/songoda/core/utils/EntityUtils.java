package com.songoda.core.utils;

import com.songoda.core.compatibility.ClassMapping;
import com.cryptomorin.xseries.XMaterial;
import com.songoda.core.nms.entity.NmsEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @deprecated Use {@link NmsEntity} instead
 */
@Deprecated
public class EntityUtils {
    private static Class<?> clazzEntityInsentient, clazzEntity, clazzCraftEntity;

    private static Field aware, fromMobSpawner;

    private static Method methodGetHandle;

    static {
        try {
            clazzEntityInsentient = ClassMapping.ENTITY_INSENTIENT.getClazz();
            clazzEntity = ClassMapping.ENTITY.getClazz();
            clazzCraftEntity = ClassMapping.CRAFT_ENTITY.getClazz();
            methodGetHandle = clazzCraftEntity.getDeclaredMethod("getHandle");
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        try {
            aware = clazzEntityInsentient.getField("aware");
        } catch (NoSuchFieldException ex) {
            try {
                fromMobSpawner = clazzEntity.getField("fromMobSpawner");
            } catch (NoSuchFieldException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    /**
     * @deprecated Use {@link NmsEntity#setMobAware(Entity, boolean)} instead
     */
    @Deprecated
    public static void setUnaware(LivingEntity entity) {
        try {
            setUnaware(methodGetHandle.invoke(clazzCraftEntity.cast(entity)));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @deprecated Use {@link NmsEntity#setMobAware(Entity, boolean)} instead
     */
    @Deprecated
    public static void setUnaware(Object entity) {
        try {
            if (aware != null) {
                aware.setBoolean(entity, false);
            } else {
                fromMobSpawner.setBoolean(entity, true);
            }
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @deprecated Use {@link NmsEntity#isAware(Entity)} instead
     */
    @Deprecated
    public static boolean isAware(LivingEntity entity) {
        try {
            return isAware(methodGetHandle.invoke(clazzCraftEntity.cast(entity)));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * @deprecated Use {@link NmsEntity#isAware(Entity)} instead
     */
    @Deprecated
    public static boolean isAware(Object entity) {
        try {
            if (aware != null) {
                return aware.getBoolean(entity);
            } else {
                return fromMobSpawner.getBoolean(entity);
            }
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    public static List<XMaterial> getSpawnBlocks(EntityType type) {
        switch (type.name()) {
            case "PIG":
            case "SHEEP":
            case "CHICKEN":
            case "COW":
            case "RABBIT":
            case "LLAMA":
            case "HORSE":
            case "CAT":
                return Collections.singletonList(XMaterial.GRASS_BLOCK);

            case "MUSHROOM_COW":
                return Collections.singletonList(XMaterial.MYCELIUM);

            case "SQUID":
            case "ELDER_GUARDIAN":
            case "COD":
            case "SALMON":
            case "PUFFERFISH":
            case "TROPICAL_FISH":
                return Collections.singletonList(XMaterial.WATER);

            case "OCELOT":
                return Arrays.asList(XMaterial.GRASS_BLOCK,
                        XMaterial.JUNGLE_LEAVES, XMaterial.ACACIA_LEAVES,
                        XMaterial.BIRCH_LEAVES, XMaterial.DARK_OAK_LEAVES,
                        XMaterial.OAK_LEAVES, XMaterial.SPRUCE_LEAVES);

            default:
                return Collections.singletonList(XMaterial.AIR);
        }
    }
}
