package com.songoda.core.lootables.gui;

import com.songoda.core.chat.AdventureUtils;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.AnvilGui;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.Loot;
import com.songoda.core.lootables.loot.LootBuilder;
import com.songoda.core.lootables.loot.LootManager;
import com.songoda.core.utils.TextUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuiLootEditor extends Gui {
    private final LootManager lootManager;
    private final Loot loot;
    private final Gui returnGui;

    public GuiLootEditor(LootManager lootManager, Loot loot, Gui returnGui) {
        super(6, returnGui);

        this.lootManager = lootManager;
        this.loot = loot;
        this.returnGui = returnGui;

        setDefaultItem(null);
        setTitle("Loot Editor");

        paint();

        setOnClose((event) ->
                lootManager.saveLootables(false));
    }

    public void paint() {
        if (this.inventory != null) {
            this.inventory.clear();
        }

        setActionForRange(0, 0, 5, 9, null);

        setButton(8, GuiUtils.createButtonItem(XMaterial.OAK_DOOR, TextUtils.formatText("&cBack")),
                (event) -> this.guiManager.showGUI(event.player, this.returnGui));

        setButton(9, GuiUtils.createButtonItem(this.loot.getMaterial() == null ? XMaterial.BARRIER : this.loot.getMaterial(),
                TextUtils.formatText("&7Current Material: &6" + (this.loot.getMaterial() != null
                        ? this.loot.getMaterial().name() : "None")), TextUtils.formatText(
                        Arrays.asList("",
                                "&8Click to set the material to",
                                "&8the material in your hand.")
                )), (event) -> {
            ItemStack stack = event.player.getInventory().getItemInMainHand();
            this.loot.setMaterial(CompatibleMaterial.getMaterial(stack.getType()).get());

            paint();
        });

        setButton(10, GuiUtils.createButtonItem(XMaterial.PAPER,
                        TextUtils.formatText("&7Name Override: &6" + (this.loot.getName() == null ? "None set" : this.loot.getName()))),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e -> {
                        this.loot.setName(gui.getInputText().trim());

                        paint();
                        e.player.closeInventory();
                    }));

                    this.guiManager.showGUI(event.player, gui);
                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, this.loot.getName()));
                });

        setButton(11, GuiUtils.createButtonItem(XMaterial.WRITABLE_BOOK,
                        TextUtils.formatText("&7Lore Override:"),
                        TextUtils.formatText(this.loot.getLore() == null ? Collections.singletonList("&6None set") : this.loot.getLore())),
                (event) -> this.guiManager.showGUI(event.player, new GuiLoreEditor(this.loot, this)));

        List<String> enchantments = new ArrayList<>();

        if (this.loot.getEnchants() != null) {
            for (Map.Entry<String, Integer> entry : this.loot.getEnchants().entrySet()) {
                enchantments.add("&6" + entry.getKey() + " " + entry.getValue());
            }
        }

        setButton(12, GuiUtils.createButtonItem(XMaterial.ENCHANTED_BOOK,
                        TextUtils.formatText("&7Enchantments:"),
                        TextUtils.formatText(enchantments.isEmpty() ? Collections.singletonList("&6None set") : enchantments)),
                (event) -> this.guiManager.showGUI(event.player, new GuiEnchantEditor(this.loot, this)));

        setButton(13, GuiUtils.createButtonItem(
                        this.loot.getBurnedMaterial() == null
                                ? XMaterial.FIRE_CHARGE
                                : this.loot.getBurnedMaterial(),
                        TextUtils.formatText("&7Current Burned Material: &6"
                                + (this.loot.getBurnedMaterial() == null
                                ? "None"
                                : this.loot.getBurnedMaterial().name())), TextUtils.formatText(
                                Arrays.asList("",
                                        "&8Click to set the burned material to",
                                        "&8the material in your hand.")
                        )),
                (event) -> {
                    ItemStack stack = event.player.getInventory().getItemInMainHand();
                    this.loot.setBurnedMaterial(CompatibleMaterial.getMaterial(stack.getType()).get());

                    paint();
                });

        setButton(14, GuiUtils.createButtonItem(XMaterial.CLOCK,
                        TextUtils.formatText("&7Chance: &6" + this.loot.getChance()),
                        TextUtils.formatText(
                                Arrays.asList("",
                                        "&8Click to edit this loots",
                                        "&8drop chance.")
                        )),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((e) -> {
                        this.loot.setChance(Double.parseDouble(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getChance())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(15, GuiUtils.createButtonItem(XMaterial.REDSTONE,
                        TextUtils.formatText("&7Min Drop Amount: &6" + this.loot.getMin())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((e) -> {
                        this.loot.setMin(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER,
                            String.valueOf(this.loot.getMin())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(16, GuiUtils.createButtonItem(XMaterial.GLOWSTONE_DUST,
                        TextUtils.formatText("&7Max Drop Amount: &6" + this.loot.getMax())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((e) -> {
                        this.loot.setMax(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getMax())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(17, GuiUtils.createButtonItem(XMaterial.REDSTONE,
                        TextUtils.formatText("&7Min Item Damage: &6" + this.loot.getDamageMin())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e) -> {
                        this.loot.setDamageMin(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getDamageMin())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(18, GuiUtils.createButtonItem(XMaterial.GLOWSTONE_DUST,
                        TextUtils.formatText("&7Max Item Damage: &6" + this.loot.getDamageMax())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);
                    gui.setAction((e) -> {
                        this.loot.setDamageMax(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getDamageMax())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(19, GuiUtils.createButtonItem(XMaterial.CHEST,
                        TextUtils.formatText("&7Allow Looting Enchantment?: &6" + this.loot.isAllowLootingEnchant())),
                (event) -> {
                    this.loot.setAllowLootingEnchant(!this.loot.isAllowLootingEnchant());

                    paint();
                    event.player.closeInventory();
                });

        setButton(20, GuiUtils.createButtonItem(XMaterial.REDSTONE,
                        TextUtils.formatText("&7Min Child Loot Min: &6" + this.loot.getChildDropCountMin())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((e) -> {
                        this.loot.setChildDropCountMin(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getChildDropCountMin())));
                    this.guiManager.showGUI(event.player, gui);
                });

        setButton(21, GuiUtils.createButtonItem(XMaterial.GLOWSTONE_DUST,
                        TextUtils.formatText("&7Min Child Loot Max: &6" + this.loot.getChildDropCountMax())),
                (event) -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((e) -> {
                        this.loot.setChildDropCountMax(Integer.parseInt(gui.getInputText()));

                        paint();
                        e.player.closeInventory();
                    });

                    gui.setInput(GuiUtils.createButtonItem(XMaterial.PAPER, String.valueOf(this.loot.getChildDropCountMax())));
                    this.guiManager.showGUI(event.player, gui);
                });

        List<String> entities = new ArrayList<>();

        if (this.loot.getOnlyDropFor() != null) {
            for (EntityType entity : this.loot.getOnlyDropFor()) {
                entities.add("&6" + entity.name());
            }
        }

        setButton(22, GuiUtils.createButtonItem(XMaterial.ENCHANTED_BOOK,
                        TextUtils.formatText("&7Only Drop For:"),
                        TextUtils.formatText(entities)),
                (event) -> this.guiManager.showGUI(event.player, new GuiEntityEditor(this.loot, this)));

        setButton(4, 0, GuiUtils.createButtonItem(XMaterial.LIME_DYE, TextUtils.formatText("&aCreate new Child Loot")),
                (event -> {
                    AnvilGui gui = new AnvilGui(event.player, this);

                    gui.setAction((event1 -> {
                        try {
                            this.loot.addChildLoots(new LootBuilder().setMaterial(XMaterial.valueOf(gui.getInputText().trim())).build());
                        } catch (IllegalArgumentException ignore) {
                            event.player.sendMessage("That is not a valid material.");
                        }

                        event.player.closeInventory();
                        paint();
                    }));

                    gui.setTitle("Enter a material");
                    this.guiManager.showGUI(event.player, gui);
                }));

        int i = 9 * 5;
        for (Loot loot : this.loot.getChildLoot()) {
            ItemStack item = loot.getMaterial() == null
                    ? XMaterial.BARRIER.parseItem() :
                    GuiUtils.createButtonItem(loot.getMaterial(), null,
                            Arrays.asList(
                                    AdventureUtils.formatComponent("&6Left click &7to edit"),
                                    AdventureUtils.formatComponent("&6Right click &7to destroy")
                            )
                    );

            setButton(i, item,
                    (event) -> {
                        if (event.clickType == ClickType.RIGHT) {
                            this.loot.removeChildLoot(loot);
                            paint();
                        } else if (event.clickType == ClickType.LEFT) {
                            this.guiManager.showGUI(event.player, new GuiLootEditor(this.lootManager, loot, this));
                        }
                    });

            ++i;
        }
    }
}
