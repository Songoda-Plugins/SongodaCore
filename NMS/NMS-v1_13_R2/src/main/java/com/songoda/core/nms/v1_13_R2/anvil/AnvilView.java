package com.songoda.core.nms.v1_13_R2.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChatMessage;
import net.minecraft.server.v1_13_R2.ContainerAnvil;
import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.IInventory;
import net.minecraft.server.v1_13_R2.PacketPlayOutOpenWindow;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnvilView extends ContainerAnvil implements CustomAnvil {
    private final EntityPlayer entity;
    private final Inventory inventory;
    private String title = "Repairing";
    private int cost = -1;
    private boolean canUse = true;
    private AnvilTextChange textChange;

    // used for setting custom inventory
    static Field mc_ContainerAnvil_repairInventory; // subcontainer with only the result
    static Field mc_ContainerAnvil_resultInventory; // full inventory
    static Field mc_ContainerAnvil_bukkitEntity;

    static {
        try {
            mc_ContainerAnvil_repairInventory = ContainerAnvil.class.getDeclaredField("repairInventory");
            mc_ContainerAnvil_repairInventory.setAccessible(true);

            mc_ContainerAnvil_resultInventory = ContainerAnvil.class.getDeclaredField("resultInventory");
            mc_ContainerAnvil_resultInventory.setAccessible(true);

            mc_ContainerAnvil_bukkitEntity = ContainerAnvil.class.getDeclaredField("bukkitEntity");
            mc_ContainerAnvil_bukkitEntity.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    public AnvilView(EntityPlayer entity, InventoryHolder holder) {
        super(entity.inventory, entity.world, new BlockPosition(0, 0, 0), entity);

        this.checkReachable = false;
        this.entity = entity;

        if (holder != null) {
            this.inventory = getBukkitView(entity, holder).getTopInventory();
        } else {
            this.inventory = getBukkitView().getTopInventory();
        }
    }

    public CraftInventoryView getBukkitView(EntityHuman player, InventoryHolder holder) {
        try {
            AnvilInventoryCustom craftInventory = new AnvilInventoryCustom(holder,
                    new Location(entity.world.getWorld(), 0, 0, 0),
                    (IInventory) mc_ContainerAnvil_repairInventory.get(this),
                    (IInventory) mc_ContainerAnvil_resultInventory.get(this), this);
            CraftInventoryView view = new CraftInventoryView(player.getBukkitEntity(), craftInventory, this);
            mc_ContainerAnvil_bukkitEntity.set(this, view);

            return view;
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }

        return getBukkitView();
    }

    @Override
    public boolean canUse(EntityHuman entityhuman) {
        return canUse;
    }

    @Override
    public void d() {
        super.d();

        if (cost >= 0) {
            this.levelCost = cost;
        }
    }

    @Override
    public void update() {
        d();
    }

    @Override
    public String getRenameText() {
        return this.renameText;
    }

    @Override
    public void setRenameText(String text) {
        this.a(text);
    }

    @Override
    public void setOnChange(AnvilTextChange handler) {
        textChange = handler;
    }

    @Override
    public String getCustomTitle() {
        return title;
    }

    @Override
    public void setCustomTitle(String title) {
        this.title = title;
    }

    @Override
    public void setLevelCost(int cost) {
        this.cost = cost;
    }

    @Override
    public int getLevelCost() {
        return cost >= 0 ? cost : this.levelCost;
    }

    @Override
    public void setCanUse(boolean bool) {
        this.canUse = bool;
    }

    @Override
    public ItemStack getLeftInput() {
        return inventory.getItem(0);
    }

    @Override
    public ItemStack getRightInput() {
        return inventory.getItem(1);
    }

    @Override
    public ItemStack getOutput() {
        return inventory.getItem(2);
    }

    @Override
    public void setLeftInput(ItemStack item) {
        inventory.setItem(0, item);
    }

    @Override
    public void setRightInput(ItemStack item) {
        inventory.setItem(1, item);
    }

    @Override
    public void setOutput(ItemStack item) {
        inventory.setItem(2, item);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open() {
        // Counter stuff that the game uses to keep track of inventories
        int id = entity.nextContainerCounter();

        // Send the packet
        entity.playerConnection.sendPacket(new PacketPlayOutOpenWindow(id, "minecraft:anvil", new ChatMessage(title != null ? title : ""), 0));

        // Set their active container to this anvil
        entity.activeContainer = this;

        // Set their active container window id to that counter stuff
        entity.activeContainer.windowId = id;

        // Add the slot listener
        entity.activeContainer.addSlotListener(entity);
    }
}
