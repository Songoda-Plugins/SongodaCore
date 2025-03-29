package com.songoda.core.lootables.gui;

import com.songoda.core.gui.AnvilGui;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.Loot;
import com.songoda.core.utils.TextUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiEnchantEditor extends Gui {
    private final Gui returnGui;
    private final Loot loot;

    public GuiEnchantEditor(Loot loot, Gui returnGui) {
        super(1, returnGui);

        this.returnGui = returnGui;
        this.loot = loot;

        setDefaultItem(null);
        setTitle("Enchantment Editor");

        paint();
    }

    public void paint() {
        Map<String, Integer> lore = this.loot.getEnchants() == null ? new HashMap<>() : new HashMap<>(this.loot.getEnchants());

        setButton(2, GuiUtils.createButtonItem(XMaterial.OAK_FENCE_GATE,
                        TextUtils.formatText("&cBack")),
                (event) -> {
                    this.guiManager.showGUI(event.player, this.returnGui);
                    ((GuiLootEditor) this.returnGui).paint();
                });
        setButton(6, GuiUtils.createButtonItem(XMaterial.OAK_FENCE_GATE,
                        TextUtils.formatText("&cBack")),
                (event) -> {
                    this.guiManager.showGUI(event.player, this.returnGui);
                    ((GuiLootEditor) this.returnGui).paint();
                });
        setButton(3, GuiUtils.createButtonItem(XMaterial.ARROW,
                        TextUtils.formatText("&aAdd new line")),
                (event -> {
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e -> {
                        if (Enchantment.getByName(gui.getInputText().toUpperCase().trim()) == null) {
                            e.player.sendMessage("That is not a valid enchantment.");
                            e.player.closeInventory();
                            return;
                        }

                        AnvilGui gui1 = new AnvilGui(event.player, this);
                        gui1.setAction((ee -> {
                            lore.put(gui.getInputText().toUpperCase().trim(), Integer.parseInt(gui1.getInputText().trim()));
                            this.loot.setEnchants(lore);
                            ee.player.closeInventory();
                            paint();
                        }));
                        gui1.setTitle("Enter a level");
                        this.guiManager.showGUI(event.player, gui1);
                    }));

                    gui.setTitle("Enter an enchant");
                    this.guiManager.showGUI(event.player, gui);
                }));

        List<String> enchantments = new ArrayList<>();

        String last = null;

        if (!lore.isEmpty()) {
            for (Map.Entry<String, Integer> entry : lore.entrySet()) {
                last = entry.getKey();
                enchantments.add("&6" + entry.getKey() + " " + entry.getValue());
            }
        }

        setItem(4, GuiUtils.createButtonItem(XMaterial.WRITABLE_BOOK,
                TextUtils.formatText("&7Enchant Override:"),
                lore.isEmpty()
                        ? TextUtils.formatText(Collections.singletonList("&cNo enchantments set..."))
                        : TextUtils.formatText(enchantments)));

        String lastFinal = last;
        setButton(5, GuiUtils.createButtonItem(XMaterial.ARROW,
                        TextUtils.formatText("&cRemove the last line")),
                (event -> {
                    lore.remove(lastFinal);
                    this.loot.setEnchants(lore);
                    paint();
                }));
    }
}
