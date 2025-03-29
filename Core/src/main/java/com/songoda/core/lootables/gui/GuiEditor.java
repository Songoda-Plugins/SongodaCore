package com.songoda.core.lootables.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.lootables.loot.LootManager;
import com.songoda.core.lootables.loot.Lootable;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuiEditor extends Gui {
    private final LootManager lootManager;

    public GuiEditor(LootManager lootManager) {
        super(6);

        this.lootManager = lootManager;

        setDefaultItem(null);
        setTitle("Lootables Overview");

        paint();
    }

    private void paint() {
        if (this.inventory != null) {
            this.inventory.clear();
        }

        setActionForRange(0, 0, 5, 9, null);

        List<Lootable> lootables = new ArrayList<>(this.lootManager.getRegisteredLootables().values());

        double itemCount = lootables.size();
        this.pages = (int) Math.max(1, Math.ceil(itemCount / 36));

        if (this.page != 1) {
            setButton(5, 2, GuiUtils.createButtonItem(XMaterial.ARROW, "Back"),
                    (event) -> {
                        this.page--;
                        paint();
                    });
        }

        if (this.page != this.pages) {
            setButton(5, 6, GuiUtils.createButtonItem(XMaterial.ARROW, "Next"),
                    (event) -> {
                        this.page++;
                        paint();
                    });
        }

        for (int i = 9; i < 45; i++) {
            int current = ((this.page - 1) * 36) - 9;
            if (current + i >= lootables.size()) {
                setItem(i, null);
                continue;
            }

            Lootable lootable = lootables.get(current + i);
            if (lootable == null) {
                continue;
            }

            setButton(i, getIcon(lootable.getKey()),
                    (event) -> this.guiManager.showGUI(event.player, new GuiLootableEditor(this.lootManager, lootable, this)));
        }
    }

    public ItemStack getIcon(String key) {
        ItemStack stack = null;
        EntityType type = EntityType.fromName(key);

        if (type != null) {
            Optional<XMaterial> material = CompatibleMaterial.getSpawnEgg(type);

            if (material.isPresent()) {
                stack = material.get().parseItem();
            }
        }

        if (stack == null) {
            stack = XMaterial.GHAST_SPAWN_EGG.parseItem();
        }

        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(key);
        stack.setItemMeta(meta);

        return stack;
    }
}
