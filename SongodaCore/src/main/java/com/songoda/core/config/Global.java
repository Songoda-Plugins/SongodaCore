package com.songoda.core.config;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.internal.ReloadHook;
import com.songoda.core.vinject.annotation.RegisterReloadHook;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

@RegisterReloadHook
public class Global implements ReloadHook {

    private static YamlConfiguration config;

    public Global() {
        onReload();
    }

    public static <T> T isFeatureEnabled(String feature, boolean defaultValue, Class<T> type) {
        return (T) config.get(feature, false);
    }

    public static int getMaxBlocksPerTick() {
        return config != null ? config.getInt("Settings.Max Blocks Per Tick", 100) : 100;
    }

    @Override
    public void onReload() {
        File file = new File(SongodaPlugin.getInstance().getDataFolder(), "global.yml");
        if (!file.exists()) {
            SongodaPlugin.getInstance().saveResource("global.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }
}
