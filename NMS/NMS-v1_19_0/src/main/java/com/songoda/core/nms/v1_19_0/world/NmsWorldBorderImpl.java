package com.songoda.core.nms.v1_19_0.world;

import com.songoda.core.nms.world.NmsWorldBorder;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NmsWorldBorderImpl implements NmsWorldBorder {
    @Override
    public void send(Player player, BorderColor color, double size, @NotNull Location center) {
        Objects.requireNonNull(center.getWorld());

        WorldBorder worldBorder = new WorldBorder();
        worldBorder.world = ((CraftWorld) center.getWorld()).getHandle();

        worldBorder.setCenter(center.getX(), center.getZ());
        worldBorder.setSize(size);
        worldBorder.setWarningTime(0);
        worldBorder.setWarningBlocks(0);

        if (color == BorderColor.GREEN) {
            worldBorder.lerpSizeBetween(size - 0.1D, size, Long.MAX_VALUE);
        } else if (color == BorderColor.RED) {
            worldBorder.lerpSizeBetween(size, size - 1.0D, Long.MAX_VALUE);
        }

        ((CraftPlayer) player).getHandle().connection.send(new ClientboundInitializeBorderPacket(worldBorder));
    }
}
