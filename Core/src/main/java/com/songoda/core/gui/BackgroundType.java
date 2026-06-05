package com.songoda.core.gui;

/**
 * Background images available for use in toast messages
 */
public enum BackgroundType {
    ADVENTURE, END, HUSBANDRY, NETHER, STONE;
    final String key;

    BackgroundType() {
        this.key = "minecraft:textures/gui/advancements/backgrounds/" + name().toLowerCase() + ".png";
    }
}
