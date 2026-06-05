package com.songoda.core.configuration.editor;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.gui.SimplePagedGui;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit all configuration files for a specific plugin
 */
public class PluginConfigGui extends SimplePagedGui {
    final JavaPlugin plugin;
    LinkedHashMap<String, MemoryConfiguration> configs = new LinkedHashMap<>();

    public PluginConfigGui(SongodaPlugin plugin) {
        this(plugin, null);
    }

    public PluginConfigGui(SongodaPlugin plugin, Gui parent) {
        super(parent);

        this.plugin = plugin;

        // collect list of plugins
        this.configs.put(plugin.getCoreConfig().getFile().getName(), plugin.getCoreConfig());
        List<Config> more = plugin.getExtraConfig();
        if (more != null && !more.isEmpty()) {
            for (Config cfg : more) {
                this.configs.put(cfg.getFile().getName(), cfg);
            }
        }

        init();
    }

    public PluginConfigGui(JavaPlugin plugin) {
        this(plugin, null);
    }

    public PluginConfigGui(JavaPlugin plugin, Gui parent) {
        super(parent);
        this.plugin = plugin;

        // collect list of plugins
        this.configs.put("config.yml", plugin.getConfig());

        try {
            // can we also grab extra config from this mysterious plugin?
            Object more = plugin.getClass().getDeclaredMethod("getExtraConfig").invoke(plugin);
            if (more instanceof List && !((List<?>) more).isEmpty()) {
                try {
                    // if we have the getExtraConfig function, we should also be able to get the file
                    Method method_Config_getFile = ((List<?>) more).get(0).getClass().getDeclaredMethod("getFile");
                    for (Object cfg : ((List<?>) more)) {
                        this.configs.put(((File) method_Config_getFile.invoke(cfg)).getName(), (MemoryConfiguration) cfg);
                    }
                } catch (Exception ex) {
                    // include a failsafe, I guess
                    ((List<?>) more).forEach(cfg -> this.configs.put("(File " + this.configs.size() + ")", (MemoryConfiguration) cfg));
                }
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            // I guess not!
        }

        init();
    }

    private void init() {
        this.blankItem = GuiUtils.getBorderItem(XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE);

        // decorate header
        this.setTitle(ChatColor.DARK_BLUE + this.plugin.getName() + " Plugin Config");
        this.setUseHeader(true);
        this.headerBackItem = this.footerBackItem = GuiUtils.getBorderItem(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem());
        this.setButton(8, GuiUtils.createButtonItem(XMaterial.OAK_DOOR, "Exit"), (event) -> event.player.closeInventory());

        // List out all config files that this plugin has
        int i = 9;
        for (Map.Entry<String, MemoryConfiguration> config : this.configs.entrySet()) {
            this.setButton(i++, GuiUtils.createButtonItem(XMaterial.BOOK, ChatColor.YELLOW + config.getKey(), "Click to edit this config"),
                    (event) -> event.manager.showGUI(event.player, new ConfigEditorGui(event.player, this.plugin, this, config.getKey(), config.getValue())));
        }
    }
}
