package com.songoda.core.gui.methods;

import com.songoda.core.gui.events.GuiDragEvent;

public interface Draggable {

    /**
     * Called when an item is dragged in the GUI.
     *
     * @param event the drag event containing details about the action
     */
    void onDrag(GuiDragEvent event);
}
