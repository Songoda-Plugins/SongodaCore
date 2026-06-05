package com.songoda.core.gui.events;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiManager;
import org.bukkit.entity.Player;

public class GuiOpenEvent extends GuiEvent {
    public GuiOpenEvent(GuiManager manager, Gui gui, Player player) {
        super(manager, gui, player);
    }
}
