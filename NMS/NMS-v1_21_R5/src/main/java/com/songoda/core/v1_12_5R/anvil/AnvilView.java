package com.songoda.core.v1_12_5R.anvil;

import com.songoda.core.nms.anvil.CustomAnvil;
import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftAbstractInventoryView;
import org.bukkit.craftbukkit.v1_21_R5.inventory.view.CraftAnvilView;
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

    static Field mc_Container_title;
    static Field mc_ContainerAnvil_bukkitEntity;

    static {
        try {
            mc_Container_title = AbstractContainerMenu.class.getDeclaredField("title");
            mc_Container_title.setAccessible(true);

            mc_ContainerAnvil_bukkitEntity = AnvilMenu.class.getDeclaredField("bukkitEntity");
            mc_ContainerAnvil_bukkitEntity.setAccessible(true);
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }
    }

    public AnvilView(int id, ServerPlayer entity, InventoryHolder holder) {
        super(entity.nextContainerCounter(), entity.getInventory(), ContainerLevelAccess.create(entity.level(), new BlockPos(0, 0, 0)));

        this.setTitle(MutableComponent.create(new TranslatableContents(this.customTitle != null ? this.customTitle : "", this.customTitle != null ? this.customTitle : "", new Object[0])));
        this.checkReachable = false;
        this.entity = entity;

        if (holder != null) {
            this.inventory = getBukkitView(entity, holder).getTopInventory();
        } else {
            this.inventory = getBukkitView().getTopInventory();
        }
    }

    public CraftAbstractInventoryView getBukkitView(Player player, InventoryHolder holder) {
        try {
            AnvilInventoryCustom craftInventory = new AnvilInventoryCustom(holder,
                    new Location(this.entity.level().getWorld(), 0, 0, 0),
                    this.inputSlots,
                    this.resultSlots,
                    this);
            CraftAnvilView view = new CraftAnvilView(player.getBukkitEntity(), craftInventory, this);
            mc_ContainerAnvil_bukkitEntity.set(this, view);

            return view;
        } catch (Exception ex) {
            Logger.getLogger(AnvilView.class.getName()).log(Level.SEVERE, "Anvil Setup Error", ex);
        }

        return getBukkitView();
    }

    @Override
    public boolean stillValid(Player entityhuman) {
        return this.canUse;
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();

        if (this.cost >= 0) {
            this.setLevelCost(this.cost);
        }

        this.textChange.onChange();
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
        this.textChange = handler;
    }

    @Override
    public String getCustomTitle() {
        return this.customTitle;
    }

    @Override
    public void setCustomTitle(String title) {
        this.customTitle = title;

        try {
            mc_Container_title.set(this, MutableComponent.create(new TranslatableContents(this.customTitle != null ? this.customTitle : "", this.customTitle != null ? this.customTitle : "", new Object[0])));
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
        if (this.cost >= 0) {
            return this.cost;
        }

        return this.getLevelCost();
    }

    @Override
    public void setCanUse(boolean bool) {
        this.canUse = bool;
    }

    @Override
    public ItemStack getLeftInput() {
        return this.inventory.getItem(0);
    }

    @Override
    public ItemStack getRightInput() {
        return this.inventory.getItem(1);
    }

    @Override
    public ItemStack getOutput() {
        return this.inventory.getItem(2);
    }

    @Override
    public void setLeftInput(ItemStack item) {
        this.inventory.setItem(0, item);
    }

    @Override
    public void setRightInput(ItemStack item) {
        this.inventory.setItem(1, item);
    }

    @Override
    public void setOutput(ItemStack item) {
        this.inventory.setItem(2, item);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public void open() {
        // Send the packet
        this.entity.connection.send(new ClientboundOpenScreenPacket(super.containerId, MenuType.ANVIL, MutableComponent.create(new TranslatableContents(this.customTitle != null ? this.customTitle : "", this.customTitle != null ? this.customTitle : "", new Object[0]))));

        // Set their active container to this anvil
        this.entity.containerMenu = this;

        // Add the slot listener
        this.entity.initMenu(this.entity.containerMenu);
    }
}
