package com.songoda.core.nms.v1_21_R7.world;

import com.songoda.core.nms.world.NmsWorldBorder;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Objects;

public class NmsWorldBorderImpl implements NmsWorldBorder {
    @Override
    public void send(Player player, BorderColor color, double size, Location center) {
        Objects.requireNonNull(center.getWorld());

        WorldBorder worldBorder = new WorldBorder();
        // We don't need this now.
        // worldBorder.world = ((CraftWorld) center.getWorld()).getHandle();

        worldBorder.setCenter(center.getX(), center.getZ());
        worldBorder.setSize(size);
        worldBorder.setWarningTime(0);
        worldBorder.setWarningBlocks(0);

        long currentTime = System.currentTimeMillis();
        if (color == BorderColor.GREEN) {
            worldBorder.lerpSizeBetween(size - 0.1D, size, currentTime, Long.MAX_VALUE);
        } else if (color == BorderColor.RED) {
            worldBorder.lerpSizeBetween(size, size - 1.0D, currentTime, Long.MAX_VALUE);
        }

        ((CraftPlayer) player).getHandle().connection.send(new ClientboundInitializeBorderPacket(worldBorder));
    }
}
