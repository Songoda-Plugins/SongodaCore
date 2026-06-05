package com.songoda.core.nms.v1_19_R1.anvil;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryAnvil;
import org.bukkit.inventory.InventoryHolder;

public class AnvilInventoryCustom extends CraftInventoryAnvil {
    final InventoryHolder holder;

    public AnvilInventoryCustom(InventoryHolder holder, Location location, Container inventory, Container resultInventory, AnvilMenu container) {
        super(location, inventory, resultInventory, container);

        this.holder = holder;
    }

    @Override
    public InventoryHolder getHolder() {
        return holder;
    }
}
