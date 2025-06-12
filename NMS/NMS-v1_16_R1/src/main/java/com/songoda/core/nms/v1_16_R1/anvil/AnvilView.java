package com.songoda.core.nms.v1_16_R1.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.ChatMessage;
import net.minecraft.server.v1_16_R1.Container;
import net.minecraft.server.v1_16_R1.ContainerAccess;
import net.minecraft.server.v1_16_R1.ContainerAnvil;
import net.minecraft.server.v1_16_R1.ContainerAnvilAbstract;
import net.minecraft.server.v1_16_R1.Containers;
import net.minecraft.server.v1_16_R1.EntityHuman;
import net.minecraft.server.v1_16_R1.EntityPlayer;
import net.minecraft.server.v1_16_R1.IInventory;
import net.minecraft.server.v1_16_R1.PacketPlayOutOpenWindow;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
            mc_ContainerAnvil_repairInventory = ContainerAnvilAbstract.class.getDeclaredField("repairInventory");
            mc_ContainerAnvil_repairInventory.setAccessible(true);

            mc_ContainerAnvil_resultInventory = ContainerAnvilAbstract.class.getDeclaredField("resultInventory");
            mc_ContainerAnvil_resultInventory.setAccessible(true);

            mc_ContainerAnvil_bukkitEntity = ContainerAnvil.class.getDeclaredField("bukkitEntity");
            mc_ContainerAnvil_bukkitEntity.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    static Method mc_ContainerProperty_set;
    static Method mc_ContainerProperty_get;
    // 1.15 made this field public again, but now it's final. idk.
    static Field mc_Container_windowId;
    // 1.14 also introduced a title field, also private, which can only be set once and can't be checked
    static Field mc_Container_title;

    static {
        try {
            mc_Container_title = Container.class.getDeclaredField("title");
            mc_Container_title.setAccessible(true);
            mc_Container_windowId = Container.class.getDeclaredField("windowId");
            mc_Container_windowId.setAccessible(true);

            // remove the final modifier
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(mc_Container_windowId, mc_Container_windowId.getModifiers() & ~Modifier.FINAL);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    public AnvilView(int id, EntityPlayer entity, InventoryHolder holder) {
        super(id, entity.inventory, ContainerAccess.at(entity.world, new BlockPosition(0, 0, 0)));

        this.setTitle(new ChatMessage(customTitle != null ? customTitle : ""));
        this.checkReachable = false;
        this.entity = entity;

        if (holder != null) {
            this.inventory = getBukkitView(entity, holder).getTopInventory();
            return;
        }

        this.inventory = getBukkitView().getTopInventory();
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
    public void e() {
        super.e();

        if (cost >= 0) {
            this.levelCost.set(cost);
        }

        textChange.onChange();
    }

    @Override
    public void update() {
        e();
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

        return this.levelCost.get();
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
        entity.playerConnection.sendPacket(new PacketPlayOutOpenWindow(id, Containers.ANVIL, new ChatMessage(customTitle != null ? customTitle : "")));

        // Set their active container to this anvil
        entity.activeContainer = this;

        try {
            // Set their active container window id to that counter stuff
            mc_Container_windowId.set(this, id);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Create Error", ex);
        }

        // Add the slot listener
        entity.activeContainer.addSlotListener(entity);
    }
}
