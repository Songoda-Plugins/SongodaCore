package com.craftaro.core.nms.v1_21_R4.anvil;

import com.craftaro.core.nms.anvil.CustomAnvil;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class AnvilCore implements com.craftaro.core.nms.anvil.AnvilCore {
    @Override
    public CustomAnvil createAnvil(Player player) {
        ServerPlayer p = ((CraftPlayer) player).getHandle();
        return new AnvilView(p.nextContainerCounter(), p, null);
    }

    @Override
    public CustomAnvil createAnvil(Player player, InventoryHolder holder) {
        ServerPlayer p = ((CraftPlayer) player).getHandle();
        return new AnvilView(p.nextContainerCounter(), p, holder);
    }
}
