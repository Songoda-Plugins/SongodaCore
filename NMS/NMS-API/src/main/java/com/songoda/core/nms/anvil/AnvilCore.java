package com.songoda.core.nms.anvil;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public interface AnvilCore {
    CustomAnvil createAnvil(Player player);

    CustomAnvil createAnvil(Player player, InventoryHolder holder);
}
