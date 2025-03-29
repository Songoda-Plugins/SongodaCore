package com.songoda.core.world;

import com.songoda.core.compatibility.ClassMapping;
import com.songoda.core.compatibility.MethodMapping;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.nms.Nms;
import com.songoda.core.nms.world.NmsWorldBorder;
import com.songoda.core.utils.NMSUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @deprecated Use {@link NmsWorldBorder} via {@link Nms#getImplementations()} instead.
 */
@Deprecated
public class SWorldBorder {
    private static Class<?> packetPlayOutWorldBorderEnumClass;
    private static Class<?> worldBorderClass;
    private static Class<?> craftWorldClass;
    private static Constructor<?> packetPlayOutWorldBorderConstructor;

    private static Constructor<?> clientboundInitializeBorderPacketConstructor;

    static {
        try {
            worldBorderClass = ClassMapping.WORLD_BORDER.getClazz();
            craftWorldClass = ClassMapping.CRAFT_WORLD.getClazz();

            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17)) {
                Class<?> clientboundInitializeBorderPacketClass = ClassMapping.CLIENTBOUND_INITIALIZE_BORDER_PACKET.getClazz();
                clientboundInitializeBorderPacketConstructor = clientboundInitializeBorderPacketClass.getConstructor(worldBorderClass);
            } else {
                Class<?> packetPlayOutWorldBorder = ClassMapping.PACKET_PLAY_OUT_WORLD_BORDER.getClazz();

                if (packetPlayOutWorldBorder != null) {
                    if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11))
                        packetPlayOutWorldBorderEnumClass = packetPlayOutWorldBorder.getDeclaredClasses()[0];
                    else
                        packetPlayOutWorldBorderEnumClass = packetPlayOutWorldBorder.getDeclaredClasses()[1];

                    packetPlayOutWorldBorderConstructor = packetPlayOutWorldBorder.getConstructor(worldBorderClass,
                            packetPlayOutWorldBorderEnumClass);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void send(Player player, Color color, double size, Location centerLocation) {
        try {
            if (centerLocation == null || centerLocation.getWorld() == null) {
                return;
            }

            Object worldBorder = worldBorderClass.getConstructor().newInstance();

            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
                Object nmsWorld = MethodMapping.CB_GENERIC__GET_HANDLE.getMethod(ClassMapping.CRAFT_WORLD.getClazz()).invoke(centerLocation.getWorld());
                NMSUtils.setField(worldBorder, "world", nmsWorld, false);
            }

            Method setCenter = MethodMapping.WORLD_BOARDER__SET_CENTER.getMethod(ClassMapping.WORLD_BORDER.getClazz());
            setCenter.invoke(worldBorder, centerLocation.getX(), centerLocation.getZ());

            Method setSize = MethodMapping.WORLD_BOARDER__SET_SIZE.getMethod(ClassMapping.WORLD_BORDER.getClazz());
            setSize.invoke(worldBorder, size);

            Method setWarningTime = MethodMapping.WORLD_BOARDER__SET_WARNING_TIME.getMethod(ClassMapping.WORLD_BORDER.getClazz());
            setWarningTime.invoke(worldBorder, 0);

            Method setWarningDistance = MethodMapping.WORLD_BOARDER__SET_WARNING_DISTANCE.getMethod(ClassMapping.WORLD_BORDER.getClazz());
            setWarningDistance.invoke(worldBorder, 0);

            Method transitionSizeBetween = MethodMapping.WORLD_BOARDER__TRANSITION_SIZE_BETWEEN.getMethod(ClassMapping.WORLD_BORDER.getClazz());

            if (color == Color.Green) {
                transitionSizeBetween.invoke(worldBorder, size - 0.1D, size, Long.MAX_VALUE);
            } else if (color == Color.Red) {
                transitionSizeBetween.invoke(worldBorder, size, size - 1.0D, Long.MAX_VALUE);
            }

            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17)) {
                Object packet = clientboundInitializeBorderPacketConstructor.newInstance(worldBorder);
                Nms.getImplementations().getPlayer().sendPacket(player, packet);
            } else {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object packet = packetPlayOutWorldBorderConstructor.newInstance(worldBorder,
                        Enum.valueOf((Class<? extends Enum>) packetPlayOutWorldBorderEnumClass, "INITIALIZE"));
                Nms.getImplementations().getPlayer().sendPacket(player, packet);
            }
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @deprecated Use {@link NmsWorldBorder.BorderColor} instead.
     */
    @Deprecated
    public enum Color {
        Blue, Green, Red
    }
}
