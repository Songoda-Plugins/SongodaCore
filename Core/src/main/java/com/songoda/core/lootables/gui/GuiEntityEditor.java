package com.songoda.core.lootables.gui;

import com.songoda.core.gui.Gui;
import com.songoda.core.lootables.loot.Loot;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class GuiEntityEditor extends AbstractGuiListEditor {
    public GuiEntityEditor(Loot loot, Gui returnGui) {
        super(loot, returnGui);
    }

    @Override
    protected List<String> getData() {
        return this.loot.getOnlyDropFor().stream().map(Enum::name).collect(Collectors.toList());
    }

    @Override
    protected void updateData(List<String> list) {
        this.loot.setOnlyDropFor(list.stream().map(EntityType::valueOf).collect(Collectors.toList()));
    }

    @Override
    protected String validate(String line) {
        line = line.toUpperCase().trim();

        try {
            EntityType.valueOf(line);
            return line;
        } catch (IllegalArgumentException ignore) {
        }

        return null;
    }
}
