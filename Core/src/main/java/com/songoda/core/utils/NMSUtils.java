package com.songoda.core.utils;

import com.songoda.core.compatibility.ClassMapping;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.nms.Nms;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @deprecated Use {@link Nms} instead
 */
@Deprecated
public class NMSUtils {
    /**
     * @deprecated Use {@link ClassMapping} instead
     */
    @Deprecated
    public static Class<?> getCraftClass(String className) {
        try {
            String fullName = "org.bukkit.craftbukkit." + ServerVersion.getServerVersionString() + "." + className;
            return Class.forName(fullName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Method getPrivateMethod(Class<?> c, String methodName, Class<?>... parameters) throws Exception {
        Method m = c.getDeclaredMethod(methodName, parameters);
        m.setAccessible(true);

        return m;
    }

    public static Field getField(Class<?> clazz, String name, boolean declared) {
        try {
            Field field;

            if (declared) {
                field = clazz.getDeclaredField(name);
            } else {
                field = clazz.getField(name);
            }

            field.setAccessible(true);
            return field;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Object getFieldObject(Object object, Field field) {
        try {
            return field.get(object);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static void setField(Object object, String fieldName, Object fieldValue, boolean declared) {
        try {
            Field field = declared ? object.getClass().getDeclaredField(fieldName) : object.getClass().getField(fieldName);
            field.setAccessible(true);

            field.set(object, fieldValue);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // FIXME: Remove this method on next major release
    /**
     * @deprecated Doesn't work cross version, use NMSManager#getPlayer() instead
     */
    @Deprecated
    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17) ? "b" : "playerConnection").get(handle);

            playerConnection.getClass().getMethod("sendPacket", ClassMapping.PACKET.getClazz()).invoke(playerConnection, packet);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
