package com.songoda.core.core;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.dependency.DependencyLoader;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluginInfo {
    private final JavaPlugin javaPlugin;
    private final int songodaId;
    private final String coreIcon;
    private final XMaterial icon;
    private final String coreLibraryVersion;

    private final List<PluginInfoModule> modules = new ArrayList<>();
    private String marketplaceLink;

    public PluginInfo(JavaPlugin javaPlugin, int songodaId, String icon, String coreLibraryVersion) {
        this.javaPlugin = javaPlugin;
        this.songodaId = songodaId;
        this.coreIcon = icon;
        this.icon = CompatibleMaterial.getMaterial(icon).orElse(XMaterial.STONE);
        this.coreLibraryVersion = coreLibraryVersion;
    }

    public String getMarketplaceLink() {
        return this.marketplaceLink;
    }

    public void setMarketplaceLink(String marketplaceLink) {
        this.marketplaceLink = marketplaceLink;
    }

    public PluginInfoModule addModule(PluginInfoModule module) {
        this.modules.add(module);

        return module;
    }

    public List<PluginInfoModule> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    public JavaPlugin getJavaPlugin() {
        return this.javaPlugin;
    }

    public int getSongodaId() {
        return this.songodaId;
    }

    public String getCoreIcon() {
        return this.coreIcon;
    }

    public XMaterial getIcon() {
        return this.icon;
    }

    public String getCoreLibraryVersion() {
        return this.coreLibraryVersion;
    }

    public int getDependencyVersion() {
        return DependencyLoader.getDependencyVersion();
    }
}
