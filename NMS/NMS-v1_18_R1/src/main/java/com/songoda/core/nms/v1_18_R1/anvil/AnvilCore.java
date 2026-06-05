package com.songoda.core.nms.v1_18_R1.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class AnvilCore implements com.songoda.core.nms.anvil.AnvilCore {
    @Override
    public CustomAnvil createAnvil(Player player) {
        return createAnvil(player, null);
    }

    @Override
    public CustomAnvil createAnvil(Player player, InventoryHolder holder) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        return new AnvilView(serverPlayer.nextContainerCounter(), serverPlayer, holder);
    }
}
