package com.songoda.core.gui.methods;

import com.songoda.core.gui.events.GuiDropItemEvent;

public interface Droppable {
    boolean onDrop(GuiDropItemEvent event);
}
