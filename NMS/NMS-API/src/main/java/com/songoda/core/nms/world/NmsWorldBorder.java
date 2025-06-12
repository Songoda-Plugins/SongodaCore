package com.songoda.core.nms.world;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NmsWorldBorder {
    void send(Player player, BorderColor color, double size, Location center);

    enum BorderColor {
        BLUE, GREEN, RED
    }
}
