package com.songoda.core.nms.v1_17_R1.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.ContainerAnvilAbstract;
import net.minecraft.world.inventory.Containers;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnvilView extends ContainerAnvil implements CustomAnvil {
    private final EntityPlayer entity;
    private final Inventory inventory;
    private String customTitle = "Repairing";
    private int cost = -1;
    private boolean canUse = true;
    private AnvilTextChange textChange;

    // used for setting custom inventory
    static Field mc_ContainerAnvil_repairInventory; // subcontainer with only the result
    static Field mc_ContainerAnvil_resultInventory; // full inventory
    static Field mc_ContainerAnvil_bukkitEntity;

    static {
        try {
            mc_ContainerAnvil_repairInventory = ContainerAnvilAbstract.class.getDeclaredField("p");
            mc_ContainerAnvil_repairInventory.setAccessible(true);

            mc_ContainerAnvil_resultInventory = ContainerAnvilAbstract.class.getDeclaredField("o");
            mc_ContainerAnvil_resultInventory.setAccessible(true);

            mc_ContainerAnvil_bukkitEntity = ContainerAnvil.class.getDeclaredField("bukkitEntity");
            mc_ContainerAnvil_bukkitEntity.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    // 1.14 also introduced a title field, also private, which can only be set once and can't be checked
    static Field mc_Container_title;

    static {
        try {
            mc_Container_title = Container.class.getDeclaredField("title");
            mc_Container_title.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    public AnvilView(int id, EntityPlayer entity, InventoryHolder holder) {
        super(entity.nextContainerCounter(), entity.getInventory(), ContainerAccess.at(entity.getWorld(), new BlockPosition(0, 0, 0)));

        this.setTitle(new ChatMessage(customTitle != null ? customTitle : ""));
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
                    new Location(entity.getWorld().getWorld(), 0, 0, 0),
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
    public void e() {
        super.e();

        if (cost >= 0) {
            this.setLevelCost(cost);
        }

        textChange.onChange();
    }

    @Override
    public void update() {
        e();
    }

    @Override
    public String getRenameText() {
        return this.v;
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
        return customTitle;
    }

    @Override
    public void setCustomTitle(String title) {
        this.customTitle = title;

        try {
            mc_Container_title.set(this, new ChatMessage(customTitle != null ? customTitle : ""));
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Error", ex);
        }
    }

    @Override
    public void setLevelCost(int cost) {
        this.cost = cost;
    }

    @Override
    public int getLevelCost() {
        if (cost >= 0) {
            return cost;
        }

        return this.getLevelCost();
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
        // Send the packet
        entity.b.sendPacket(new PacketPlayOutOpenWindow(j, Containers.h, new ChatMessage(customTitle != null ? customTitle : "")));

        // Set their active container to this anvil
        entity.bV = this;

        // Add the slot listener
        entity.initMenu(entity.bV);
        //entity.bV.addSlotListener(entity.cX);
    }
}
