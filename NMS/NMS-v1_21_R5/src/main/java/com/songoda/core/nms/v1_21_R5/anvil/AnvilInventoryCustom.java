package com.songoda.core.nms.v1_21_R5.anvil;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftInventoryAnvil;
import org.bukkit.inventory.InventoryHolder;

public class AnvilInventoryCustom extends CraftInventoryAnvil {
    final InventoryHolder holder;

    public AnvilInventoryCustom(InventoryHolder holder, Location location, Container inventory, Container resultInventory, AnvilMenu container) {
        super(location, inventory, resultInventory);

        this.holder = holder;
    }

    @Override
    public InventoryHolder getHolder() {
        return this.holder;
    }
}
