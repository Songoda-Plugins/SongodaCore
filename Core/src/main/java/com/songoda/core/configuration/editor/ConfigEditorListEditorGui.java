package com.songoda.core.configuration.editor;

import com.songoda.core.gui.GuiUtils;
import com.songoda.core.gui.SimplePagedGui;
import com.songoda.core.input.ChatPrompt;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * Edit a string list
 */
public class ConfigEditorListEditorGui extends SimplePagedGui {
    final ConfigEditorGui current;

    public boolean saveChanges = false;
    public List<String> values;

    public ConfigEditorListEditorGui(ConfigEditorGui current, String key, List<String> val) {
        super(current);

        this.current = current;
        this.blankItem = current.getDefaultItem();
        this.headerBackItem = this.footerBackItem = current.getHeaderBackItem();
        setTitle(ChatColor.DARK_BLUE + "String List Editor");
        this.setUseHeader(true);
        this.setItem(4, current.configItem(XMaterial.FILLED_MAP, key, current.getCurrentNode(), key, null));
        this.setButton(8, GuiUtils.createButtonItem(XMaterial.OAK_DOOR, "Exit"), (event) -> event.player.closeInventory());
        this.values = new ArrayList<>(val);

        this.setButton(8, GuiUtils.createButtonItem(XMaterial.LAVA_BUCKET, ChatColor.RED + "Discard Changes"), (event) -> event.player.closeInventory());
        this.setButton(0, GuiUtils.createButtonItem(XMaterial.REDSTONE, ChatColor.GREEN + "Save"), (event) -> {
            this.saveChanges = true;
            event.player.closeInventory();
        });
        this.setButton(1, GuiUtils.createButtonItem(XMaterial.CHEST, ChatColor.BLUE + "Add Item"),
                (event) -> {
                    event.gui.exit();
                    ChatPrompt.showPrompt(event.manager.getPlugin(), event.player, "Enter a new value to add:", response -> {
                                this.values.add(response.getMessage().trim());
                                redraw();
                            })
                            .setOnClose(() -> event.manager.showGUI(event.player, this))
                            .setOnCancel(() -> {
                                event.player.sendMessage(ChatColor.RED + "Edit canceled");
                                event.manager.showGUI(event.player, this);
                            });
                });

        redraw();
    }

    void redraw() {
        this.page = 1;

        // clear old display
        if (this.inventory != null) {
            for (Integer i : this.cellItems.keySet().toArray(new Integer[0])) {
                if (i > 8) {
                    this.cellItems.remove(i);
                    this.conditionalButtons.remove(i);
                }
            }
        }

        // update items
        int i = 9;
        for (String item : this.values) {
            final int index = i - 9;
            setButton(i++, GuiUtils.createButtonItem(XMaterial.PAPER, item, "Right-click to remove"), ClickType.RIGHT, (event) -> {
                this.values.remove(index);
                redraw();
            });
        }

        // update display
        update();
    }
}
