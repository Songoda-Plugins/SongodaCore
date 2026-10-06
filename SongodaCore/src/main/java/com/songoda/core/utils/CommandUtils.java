package com.songoda.core.utils;

import com.songoda.core.SongodaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public class CommandUtils {

    public static void registerCommand(String command, CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        try {
            // Retrieve the SimpleCommandMap from the server
            Class<?> clazzCraftServer = Bukkit.getServer().getClass();
            Object craftServer = clazzCraftServer.cast(Bukkit.getServer());
            SimpleCommandMap commandMap = (SimpleCommandMap) craftServer.getClass()
                    .getDeclaredMethod("getCommandMap").invoke(craftServer);

            // Construct a new BaseCommand object
            Constructor<PluginCommand> constructorPluginCommand = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructorPluginCommand.setAccessible(true);
            PluginCommand commandObject = constructorPluginCommand.newInstance(command, SongodaPlugin.getInstance());

            // Set command action
            commandObject.setExecutor(executor);

            //Set permission if provided
            if (permission != null && !permission.isEmpty()) {
                commandObject.setPermission(permission);
            }

            // Set tab complete
            commandObject.setTabCompleter(tabCompleter);

            // Register the command
            Field fieldKnownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            fieldKnownCommands.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) fieldKnownCommands.get(commandMap);
            knownCommands.put(command, commandObject);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }
}
