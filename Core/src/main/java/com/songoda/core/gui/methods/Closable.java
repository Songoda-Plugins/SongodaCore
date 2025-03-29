package com.songoda.core.gui.methods;

import com.songoda.core.gui.events.GuiCloseEvent;

public interface Closable {
    void onClose(GuiCloseEvent event);
}
