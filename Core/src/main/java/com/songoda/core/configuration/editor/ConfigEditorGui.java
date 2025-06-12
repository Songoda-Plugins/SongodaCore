package com.songoda.core.configuration.editor;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.gui.SimplePagedGui;
import com.songoda.core.input.ChatPrompt;
import com.songoda.core.utils.ItemUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Edit a configuration file for a specific plugin
 */
public class ConfigEditorGui extends SimplePagedGui {
    final JavaPlugin plugin;
    final String file;
    final MemoryConfiguration config;
    final ConfigurationSection node;
    final Player player;
    Method configSection_getCommentString = null;
    boolean edits = false;
    List<String> sections = new ArrayList<>();
    List<String> settings = new ArrayList<>();

    protected ConfigEditorGui(Player player, JavaPlugin plugin, Gui parent, String file, MemoryConfiguration config) {
        this(player, plugin, parent, file, config, config);
    }

    protected ConfigEditorGui(Player player, JavaPlugin plugin, Gui parent, String file, MemoryConfiguration config, ConfigurationSection node) {
        super(parent);

        this.player = player;
        this.plugin = plugin;
        this.file = file;
        this.config = config;
        this.node = node;
        this.blankItem = GuiUtils.getBorderItem(XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE);

        if (!(parent instanceof ConfigEditorGui)) {
            setOnClose((gui) -> save());
        } else {
            setOnClose((gui) -> ((ConfigEditorGui) parent).edits |= this.edits);
        }

        // if we have a ConfigSection, we can also grab comments
        try {
            this.configSection_getCommentString = node.getClass().getDeclaredMethod("getCommentString", String.class);
        } catch (Exception ignore) {
        }

        // decorate header
        this.setTitle(ChatColor.DARK_BLUE + file);
        this.setUseHeader(true);
        this.headerBackItem = this.footerBackItem = GuiUtils.getBorderItem(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem());
        final String path = node.getCurrentPath();
        this.setItem(4, configItem(XMaterial.FILLED_MAP, !path.isEmpty() ? path : file, config, !path.isEmpty() ? path : null, ChatColor.BLACK.toString()));
        this.setButton(8, GuiUtils.createButtonItem(XMaterial.OAK_DOOR, "Exit"), (event) -> event.player.closeInventory());

        // compile list of settings
        for (String key : node.getKeys(false)) {
            if (node.isConfigurationSection(key)) {
                this.sections.add(key);
                continue;
            }

            this.settings.add(key);
        }

        // next we need to display the config settings
        int index = 9;
        for (final String sectionKey : this.sections) {
            setButton(index++, configItem(XMaterial.WRITABLE_BOOK, ChatColor.YELLOW + sectionKey, node, sectionKey, "Click to open this section"),
                    (event) -> event.manager.showGUI(event.player, new ConfigEditorGui(player, plugin, this, file, config, node.getConfigurationSection(sectionKey))));
        }

        // now display individual settings
        for (final String settingKey : this.settings) {
            final Object val = node.get(settingKey);
            if (val == null) {
                continue;
            }

            if (val instanceof Boolean) {
                // toggle switch
                setButton(index, configItem(XMaterial.LEVER, ChatColor.YELLOW + settingKey, node, settingKey, String.valueOf(val), "Click to toggle this setting"),
                        (event) -> this.toggle(event.slot, settingKey));

                if ((Boolean) val) {
                    highlightItem(index);
                }
            } else if (isNumber(val)) {
                // number dial
                this.setButton(index, configItem(XMaterial.CLOCK, ChatColor.YELLOW + settingKey, node, settingKey, String.valueOf(val), "Click to edit this setting"),
                        (event) -> {
                            event.gui.exit();
                            ChatPrompt.showPrompt(plugin, event.player, "Enter a new number value for " + settingKey + ":", response -> {
                                        if (!setNumber(event.slot, settingKey, response.getMessage().trim())) {
                                            event.player.sendMessage(ChatColor.RED + "Error: \"" + response.getMessage().trim() + "\" is not a number!");
                                        }
                                    }).setOnClose(() -> event.manager.showGUI(event.player, this))
                                    .setOnCancel(() -> {
                                        event.player.sendMessage(ChatColor.RED + "Edit canceled");
                                        event.manager.showGUI(event.player, this);
                                    });
                        });
            } else if (isMaterial(val)) {
                // changing a block
                // isMaterial is more of a guess, to be honest.
                setButton(index, configItem(XMaterial.STONE, ChatColor.YELLOW + settingKey, node, settingKey, val.toString(), "Click to edit this setting"),
                        (event) -> {
                            SimplePagedGui paged = new SimplePagedGui(this);
                            paged.setTitle(ChatColor.BLUE + settingKey);
                            paged.setHeaderBackItem(this.headerBackItem).setFooterBackItem(this.footerBackItem).setDefaultItem(this.blankItem);
                            paged.setItem(4, configItem(XMaterial.FILLED_MAP, settingKey, node, settingKey, "Choose an item to change this value to"));
                            int i = 9;
                            for (XMaterial mat : getAllValidMaterials()) {
                                try {
                                    ItemStack buttonItem = GuiUtils.createButtonItem(mat, mat.name());
                                    if (!buttonItem.getType().isItem()) continue;

                                    paged.setButton(i++, buttonItem, ClickType.LEFT, (matEvent) -> {
                                        setMaterial(event.slot, settingKey, matEvent.clickedItem);
                                        matEvent.player.closeInventory();
                                    });
                                } catch (IllegalArgumentException ex) {
                                    // FIXME: CompatibleMaterial is not working properly for 'ZOMBIE_PIGMAN_SPAWN_EGG'
                                }
                            }
                            event.manager.showGUI(event.player, paged);
                        });
            } else if (val instanceof String) {
                // changing a "string" value (or change to a feather for writing quill)
                setButton(index, configItem(XMaterial.STRING, ChatColor.YELLOW + settingKey, node, settingKey, val.toString(), "Click to edit this setting"),
                        (event) -> {
                            event.gui.exit();
                            ChatPrompt.showPrompt(plugin, event.player, "Enter a new value for " + settingKey + ":", response -> {
                                        node.set(settingKey, response.getMessage().trim());
                                        updateValue(event.slot, settingKey);
                                    }).setOnClose(() -> event.manager.showGUI(event.player, this))
                                    .setOnCancel(() -> {
                                        event.player.sendMessage(ChatColor.RED + "Edit canceled");
                                        event.manager.showGUI(event.player, this);
                                    });
                        });
            } else if (val instanceof List) {
                setButton(index, configItem(XMaterial.WRITABLE_BOOK, ChatColor.YELLOW + settingKey, node, settingKey, String.format("(%d values)", ((List<?>) val).size()), "Click to edit this setting"),
                        (event) ->
                                event.manager.showGUI(event.player, (new ConfigEditorListEditorGui(this, settingKey, (List) val)).setOnClose((gui) -> {
                                    if (((ConfigEditorListEditorGui) gui.gui).saveChanges) {
                                        setList(event.slot, settingKey, ((ConfigEditorListEditorGui) gui.gui).values);
                                    }
                                })));
            } /* else {
                // idk. should we display uneditable values?
            }  */

            ++index;
        }
    }

    public ConfigurationSection getCurrentNode() {
        return this.node;
    }

    protected void updateValue(int clickCell, String path) {
        ItemStack item = this.inventory.getItem(clickCell);

        if (item == null || item == AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        Object val = this.node.get(path);

        if (meta != null && val != null) {
            String valStr;
            if (val instanceof List) {
                valStr = String.format("(%d values)", ((List<?>) val).size());
            } else {
                valStr = val.toString();
            }

            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) {
                meta.setLore(Arrays.asList(valStr));
            } else {
                lore.set(0, valStr);
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
            setItem(clickCell, item);
        }

        this.edits = true;
    }

    void toggle(int clickCell, String path) {
        boolean val = !this.node.getBoolean(path);
        this.node.set(path, val);

        if (val) {
            setItem(clickCell, ItemUtils.addGlow(this.inventory.getItem(clickCell)));
        } else {
            setItem(clickCell, ItemUtils.removeGlow(this.inventory.getItem(clickCell)));
        }

        updateValue(clickCell, path);
    }

    boolean setNumber(int clickCell, String path, String input) {
        try {
            if (this.node.isInt(path)) {
                this.node.set(path, Integer.parseInt(input));
            } else if (this.node.isDouble(path)) {
                this.node.set(path, Double.parseDouble(input));
            } else if (this.node.isLong(path)) {
                this.node.set(path, Long.parseLong(input));
            }

            updateValue(clickCell, path);
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }

    void setMaterial(int clickCell, String path, ItemStack item) {
        Optional<XMaterial> mat = CompatibleMaterial.getMaterial(item.getType());

        if (!mat.isPresent()) {
            this.node.set(path, XMaterial.STONE.name());
        } else {
            this.node.set(path, mat.get().name());
        }

        updateValue(clickCell, path);
    }

    void setList(int clickCell, String path, List<String> list) {
        this.node.set(path, list);
        updateValue(clickCell, path);
    }

    void save() {
        if (!this.edits) {
            return;
        }

        // could also check and call saveChanges()
        if (this.config instanceof FileConfiguration) {
            try {
                ((FileConfiguration) this.config).save(new File(this.plugin.getDataFolder(), this.file));
            } catch (IOException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to save config changes to " + this.file, ex);
                return;
            }
        } else if (this.config instanceof Config) {
            ((Config) this.config).save();
        } else {
            this.player.sendMessage(ChatColor.RED + "Unknown configuration type '" + this.config.getClass().getName() + "' - Please report this error!");
            this.plugin.getLogger().log(Level.WARNING, "Unknown configuration type '" + this.config.getClass().getName() + "' - Please report this error!");
            return;
        }

        this.plugin.reloadConfig();
        this.player.sendMessage(ChatColor.GREEN + "Config " + this.file + " saved!");
    }

    private boolean isNumber(Object value) {
        return (value instanceof Long
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Double);
    }

    private boolean isMaterial(Object value) {
        if (!(value instanceof String && value.toString().equals(value.toString().toUpperCase()))) {
            return false;
        }

        Optional<XMaterial> material = CompatibleMaterial.getMaterial(value.toString());
        return material.isPresent() && material.get().isSupported();
    }

    protected ItemStack configItem(XMaterial type, String name, ConfigurationSection node, String path, String def) {
        String[] info = null;

        if (this.configSection_getCommentString != null) {
            try {
                Object comment = this.configSection_getCommentString.invoke(node, path);

                if (comment != null) {
                    info = comment.toString().split("\n");
                }
            } catch (Exception ignore) {
            }
        }

        return GuiUtils.createButtonItem(type, name, info != null ? info : (def != null ? def.split("\n") : null));
    }

    protected ItemStack configItem(XMaterial type, String name, ConfigurationSection node, String path, String value, String def) {
        if (value == null) {
            value = "";
        }

        String[] info = null;

        if (this.configSection_getCommentString != null) {
            try {
                Object comment = this.configSection_getCommentString.invoke(node, path);
                if (comment != null) {
                    info = (value + "\n" + comment).split("\n");
                }
            } catch (Exception ignore) {
            }
        }

        return GuiUtils.createButtonItem(type, name, info != null ? info : (def != null ? (value + "\n" + def).split("\n") : null));
    }

    private List<XMaterial> getAllValidMaterials() {
        List<XMaterial> materials = new LinkedList<>();
        for (XMaterial material : XMaterial.values()) {
            if (material.isSupported()) {
                materials.add(material);
            }
        }
        return materials;
    }
}
