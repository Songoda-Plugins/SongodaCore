package com.songoda.core.core;

import com.songoda.core.SongodaCore;
import com.songoda.core.configuration.editor.PluginConfigGui;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

final class SongodaCoreOverviewGUI extends Gui {
    SongodaCoreOverviewGUI() {
        List<PluginInfo> plugins = SongodaCore.getPlugins();
        // could do pages, too, but don't think we'll have that many at a time for a while
        int max = (int) Math.ceil(plugins.size() / 9.);
        setRows(max);
        setTitle("Songoda Plugins");

        // TODO: this could use some decorating

        for (int i = 0; i < plugins.size(); ++i) {
            final PluginInfo plugin = plugins.get(i);

            boolean hasMarketplaceLink = plugin.getMarketplaceLink() != null && !plugin.getMarketplaceLink().isEmpty();

            setButton(i, GuiUtils.createButtonItem(plugin.getIcon() != null ? plugin.getIcon() : XMaterial.STONE,
                            ChatColor.GOLD + plugin.getJavaPlugin().getName(),
                            ChatColor.GRAY + "Installed Version: " + plugin.getJavaPlugin().getDescription().getVersion(),
                            "",
                            hasMarketplaceLink ? (ChatColor.GOLD + "Click for the marketplace page link.") : "",
                            ChatColor.GOLD + "Right Click to edit plugin settings."
                    ),
                    ClickType.RIGHT, (event) -> event.manager.showGUI(event.player, new PluginConfigGui(plugin.getJavaPlugin(), event.gui)));
            if (hasMarketplaceLink) {
                setAction(i, ClickType.LEFT, (event) -> event.player.sendMessage(plugin.getMarketplaceLink()));
            }
        }
    }
}
