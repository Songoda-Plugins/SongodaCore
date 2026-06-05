package com.songoda.core.nms.v1_18_R2.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnvilView extends AnvilMenu implements CustomAnvil {
    private final ServerPlayer entity;
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
            mc_ContainerAnvil_repairInventory = ItemCombinerMenu.class.getDeclaredField("p");
            mc_ContainerAnvil_repairInventory.setAccessible(true);

            mc_ContainerAnvil_resultInventory = ItemCombinerMenu.class.getDeclaredField("o");
            mc_ContainerAnvil_resultInventory.setAccessible(true);

            mc_ContainerAnvil_bukkitEntity = AnvilMenu.class.getDeclaredField("bukkitEntity");
            mc_ContainerAnvil_bukkitEntity.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    // 1.14 also introduced a title field, also private, which can only be set once and can't be checked
    static Field mc_Container_title;

    static {
        try {
            mc_Container_title = AbstractContainerMenu.class.getDeclaredField("title");
            mc_Container_title.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    public AnvilView(int id, ServerPlayer entity, InventoryHolder holder) {
        super(entity.nextContainerCounter(), entity.getInventory(), ContainerLevelAccess.create(entity.level, new BlockPos(0, 0, 0)));

        this.setTitle(new TranslatableComponent(customTitle != null ? customTitle : ""));
        this.checkReachable = false;
        this.entity = entity;

        if (holder != null) {
            this.inventory = getBukkitView(entity, holder).getTopInventory();
        } else {
            this.inventory = getBukkitView().getTopInventory();
        }
    }

    public CraftInventoryView getBukkitView(Player player, InventoryHolder holder) {
        try {
            AnvilInventoryCustom craftInventory = new AnvilInventoryCustom(holder,
                    new Location(entity.level.getWorld(), 0, 0, 0),
                    (Container) mc_ContainerAnvil_repairInventory.get(this),
                    (Container) mc_ContainerAnvil_resultInventory.get(this), this);
            CraftInventoryView view = new CraftInventoryView(player.getBukkitEntity(), craftInventory, this);
            mc_ContainerAnvil_bukkitEntity.set(this, view);

            return view;
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }

        return getBukkitView();
    }

    @Override
    public boolean stillValid(Player entityHuman) {
        return canUse;
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();

        if (cost >= 0) {
            this.setLevelCost(cost);
        }

        textChange.onChange();
    }

    @Override
    public void update() {
        broadcastFullState();
    }

    @Override
    public String getRenameText() {
        return this.itemName;
    }

    @Override
    public void setRenameText(String text) {
        this.setItemName(text);
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
            mc_Container_title.set(this, new TranslatableComponent(customTitle != null ? customTitle : ""));
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
        entity.connection.send(new ClientboundOpenScreenPacket(super.containerId, MenuType.ANVIL, new TranslatableComponent(customTitle != null ? customTitle : "")));

        // Set their active container to this anvil
        entity.containerMenu = this;

        // Add the slot listener
        entity.initMenu(entity.containerMenu);
    }
}
