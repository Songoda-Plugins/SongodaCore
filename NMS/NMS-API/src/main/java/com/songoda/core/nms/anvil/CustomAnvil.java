package com.songoda.core.nms.anvil;

import com.songoda.core.nms.anvil.methods.AnvilTextChange;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * NMS interface for creating an anvil view for a single player
 */
public interface CustomAnvil {
    void setLevelCost(int cost);

    int getLevelCost();

    void setCanUse(boolean bool);

    String getCustomTitle();

    void setCustomTitle(String title);

    String getRenameText();

    void setRenameText(String text);

    void setOnChange(AnvilTextChange handler);

    ItemStack getLeftInput();

    ItemStack getRightInput();

    ItemStack getOutput();

    void setLeftInput(ItemStack item);

    void setRightInput(ItemStack item);

    void setOutput(ItemStack item);

    Inventory getInventory();

    /**
     * Open this anvil for the provided player
     */
    void open();

    /**
     * Forces a re-draw of the output
     */
    void update();
}
