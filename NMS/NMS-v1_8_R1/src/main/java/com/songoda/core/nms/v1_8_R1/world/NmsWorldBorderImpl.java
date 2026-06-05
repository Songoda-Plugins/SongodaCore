package com.songoda.core.nms.v1_8_R1.world;

import com.songoda.core.nms.world.NmsWorldBorder;
import net.minecraft.server.v1_8_R1.EnumWorldBorderAction;
import net.minecraft.server.v1_8_R1.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_8_R1.WorldBorder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NmsWorldBorderImpl implements NmsWorldBorder {
    @Override
    public void send(Player player, BorderColor color, double size, @NotNull Location center) {
        Objects.requireNonNull(center.getWorld());

        WorldBorder worldBorder = new WorldBorder();
        worldBorder.c(center.getX(), center.getZ());
        worldBorder.a(size);
        worldBorder.b(0);   // WarningTime
        worldBorder.c(0);   // WarningBlocks

        if (color == BorderColor.GREEN) {
            worldBorder.a(size - 0.1D, size, Long.MAX_VALUE);   // transitionSizeBetween
        } else if (color == BorderColor.RED) {
            worldBorder.a(size, size - 1.0D, Long.MAX_VALUE);   // transitionSizeBetween
        }

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldBorder(worldBorder, EnumWorldBorderAction.INITIALIZE));
    }
}
