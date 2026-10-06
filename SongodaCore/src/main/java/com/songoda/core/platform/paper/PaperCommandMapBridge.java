package com.songoda.core.platform.paper;

import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.spi.CommandMapBridge;
import com.songoda.core.spi.CommandMaps;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@net.vortexdevelopment.vinject.annotation.component.Component
public class PaperCommandMapBridge implements CommandMapBridge {

    @PostConstruct
    public void registerBridge() {
        if (!ServerProject.isPaperCompatible()) {
            return;
        }
        CommandMaps.install(this);
    }

    @Override
    public void register(Plugin plugin, PluginCommand command) {
        CommandMap commandMap = resolveCommandMap();
        commandMap.register(plugin.getName(), command);
    }

    private CommandMap resolveCommandMap() {
        Object craftServer = Bukkit.getServer();

        // Prefer server-level accessor when present.
        try {
            Method getter = craftServer.getClass().getMethod("getCommandMap");
            Object resolved = getter.invoke(craftServer);
            if (resolved instanceof CommandMap map) {
                return map;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        // Fallback for runtimes where command map is exposed on the plugin manager.
        try {
            PluginManager pluginManager = Bukkit.getPluginManager();
            Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            Object resolved = commandMapField.get(pluginManager);
            if (resolved instanceof CommandMap map) {
                return map;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException(
                "Unable to resolve Bukkit CommandMap on this server runtime for dynamic command registration");
    }
}
