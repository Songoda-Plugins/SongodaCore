package com.songoda.core.gui;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GuiItem {

    private ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> clickConsumer;
    private Consumer<ItemStackBuilder> constructItem;
    private State state = State.STANDBY;
    @Getter
    private ItemStack item;
    @Getter
    private int x;
    @Getter
    private int y;
    //Add some placeholders that can be refreshed whem the gui is shown to the player


    //Static Item
    public GuiItem(ItemStack item, int x, int y) {
        this.item = item;
        this.clickConsumer = null;
        this.x = x;
        this.y = y;
    }

    public GuiItem(ItemStack item, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> clickConsumer, int x, int y) {
        this.item = item;
        this.clickConsumer = clickConsumer;
        this.x = x;
        this.y = y;
    }

    public GuiItem(Consumer<ItemStackBuilder> itemStackBuilder, ClickConsumer<InventoryClickEvent, GuiHolder, GuiItem> clickConsumer, int x, int y) {
        ItemStackBuilder builder = new ItemStackBuilder();
        this.constructItem = itemStackBuilder;
        this.clickConsumer = clickConsumer;
        constructItem.accept(builder);
        this.item = builder.build();
        this.x = x;
        this.y = y;
    }

    public void remove() {
        this.item = null;
        this.state = State.REMOVE;
    }

    public void update() {
        if (constructItem != null) {
            this.state = State.UPDATE;
        }
    }

    public void setX(int x) {
        this.x = x;
        this.state = State.UPDATE;
    }

    public void setY(int y) {
        this.y = y;
        this.state = State.UPDATE;
    }

    void onClick(InventoryClickEvent event, GuiHolder gui, GuiItem guiItem) {
        if (this.clickConsumer != null) {
            this.clickConsumer.accept(event, gui, guiItem);
        }
    }

    boolean shouldUpdate() {
        return this.state == State.UPDATE || this.state == State.REMOVE;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        if (constructItem != null && autoUpdate) {
            this.state = State.AUTO_UPDATE;
        }
        if (!autoUpdate && this.state == State.AUTO_UPDATE) {
            this.state = State.STANDBY;
        }
    }

    public State getState() {
        return state;
    }

    ItemStack updateItem() {
        if (this.state == State.AUTO_UPDATE) {
            ItemStackBuilder builder = new ItemStackBuilder();
            constructItem.accept(builder);
            this.item = builder.build();
            return item;
        }
        if (constructItem != null) {
            ItemStackBuilder builder = new ItemStackBuilder();
            constructItem.accept(builder);
            this.item = builder.build();
            this.state = State.STANDBY;
            return item;
        }
        if (this.state == State.REMOVE) {
            this.item = new ItemStack(Material.AIR);
            this.state = State.STANDBY;
            return item;
        }
        return getItem();
    }
}
