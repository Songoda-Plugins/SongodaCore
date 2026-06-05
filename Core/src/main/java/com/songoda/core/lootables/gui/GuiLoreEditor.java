package com.songoda.core.lootables.gui;

import com.songoda.core.gui.Gui;
import com.songoda.core.lootables.loot.Loot;

import java.util.List;

public class GuiLoreEditor extends AbstractGuiListEditor {
    public GuiLoreEditor(Loot loot, Gui returnGui) {
        super(loot, returnGui);
    }

    @Override
    protected List<String> getData() {
        return this.loot.getLore();
    }

    @Override
    protected void updateData(List<String> list) {
        this.loot.setLore(list);
    }

    @Override
    protected String validate(String line) {
        return line.trim();
    }
}
