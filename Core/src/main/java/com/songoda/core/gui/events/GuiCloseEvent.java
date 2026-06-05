package com.songoda.core.gui.events;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import org.bukkit.entity.Player;

public class GuiCloseEvent extends GuiEvent {
    public GuiCloseEvent(GuiManager manager, Gui gui, Player player) {
        super(manager, gui, player);
    }
}
