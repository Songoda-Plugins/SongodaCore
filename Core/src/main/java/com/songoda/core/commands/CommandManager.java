package com.songoda.core.commands;

import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;

    private final HashMap<String, SimpleNestedCommand> commands = new HashMap<>();
    private String msg_noConsole = ChatColor.RED + "You must be a player to use this command.";
    private String msg_noPerms = ChatColor.RED + "You do not have permission to do that.";
    private String msg_noCommand = ChatColor.GRAY + "The command you entered does not exist or is spelt incorrectly.";
    private List<String> msg_syntaxError = Arrays.asList(
            ChatColor.RED + "Invalid Syntax!",
            ChatColor.GRAY + "The valid syntax is: " + ChatColor.GOLD + "%syntax%" + ChatColor.GRAY + "."
    );

    private boolean allowLooseCommands = false;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setNoConsoleMessage(String msg_noConsole) {
        this.msg_noConsole = msg_noConsole;
    }

    public void setNoPermsMessage(String msg_noPerms) {
        this.msg_noPerms = msg_noPerms;
    }

    public void setNoCommandMessage(String msg_noCommand) {
        this.msg_noCommand = msg_noCommand;
    }

    public void setSyntaxErrorMessage(List<String> msg_syntaxError) {
        this.msg_syntaxError = msg_syntaxError;
    }

    public Set<String> getCommands() {
        return Collections.unmodifiableSet(this.commands.keySet());
    }

    public List<String> getSubCommands(String command) {
        SimpleNestedCommand nested = command == null ? null : this.commands.get(command.toLowerCase());

        if (nested == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(nested.children.keySet());
    }

    public Set<AbstractCommand> getAllCommands() {
        HashSet<AbstractCommand> all = new HashSet<>();

        this.commands.values()
                .stream()
                .filter(cmd -> cmd.parent != null && !all.contains(cmd.parent))
                .forEach(cmd -> {
                    all.add(cmd.parent);

                    cmd.children.values()
                            .stream()
                            .filter(child -> !all.contains(child))
                            .forEach(all::add);
                });

        return all;
    }

    public CommandManager registerCommandDynamically(String command) {
        CommandManager.registerCommandDynamically(this.plugin, command, this, this);
        return this;
    }

    public SimpleNestedCommand registerCommandDynamically(AbstractCommand abstractCommand) {
        SimpleNestedCommand nested = new SimpleNestedCommand(abstractCommand);

        abstractCommand.getCommands().forEach(cmd -> {
            CommandManager.registerCommandDynamically(this.plugin, cmd, this, this);
            this.commands.put(cmd.toLowerCase(), nested);
            PluginCommand pcmd = this.plugin.getCommand(cmd);

            if (pcmd != null) {
                pcmd.setExecutor(this);
                pcmd.setTabCompleter(this);
            } else {
                this.plugin.getLogger().warning("Failed to register command: /" + cmd);
            }
        });

        return nested;
    }

    public SimpleNestedCommand addCommand(AbstractCommand abstractCommand) {
        SimpleNestedCommand nested = new SimpleNestedCommand(abstractCommand);

        abstractCommand.getCommands().forEach(cmd -> {
            this.commands.put(cmd.toLowerCase(), nested);
            PluginCommand pluginCommand = this.plugin.getCommand(cmd);

            if (pluginCommand == null) {
                this.plugin.getLogger().warning("Failed to register command: /" + cmd);
                return;
            }

            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(this);
        });

        return nested;
    }

    public MainCommand addMainCommand(String command) {
        MainCommand nested = new MainCommand(this.plugin, command);
        this.commands.put(command.toLowerCase(), nested.nestedCommands);

        PluginCommand pluginCommand = this.plugin.getCommand(command);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(this);
        } else {
            this.plugin.getLogger().warning("Failed to register command: /" + command);
        }

        return nested;
    }

    public MainCommand getMainCommand(String command) {
        SimpleNestedCommand nested = command == null ? null : this.commands.get(command.toLowerCase());

        if (nested != null && nested.parent instanceof MainCommand) {
            return (MainCommand) nested.parent;
        }

        return null;
    }

    public CommandManager addCommands(AbstractCommand... abstractCommands) {
        for (AbstractCommand abstractCommand : abstractCommands) {
            addCommand(abstractCommand);
        }

        return this;
    }

    public CommandManager setExecutor(String command) {
        PluginCommand pluginCommand = command == null ? null : this.plugin.getCommand(command);

        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
        } else {
            this.plugin.getLogger().warning("Failed to register command: /" + command);
        }

        return this;
    }

    public CommandManager setUseClosestCommand(boolean allowLooseCommands) {
        this.allowLooseCommands = allowLooseCommands;
        return this;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        // grab the specific command that's being called
        SimpleNestedCommand nested = this.commands.get(command.getName().toLowerCase());

        if (nested != null) {
            // check to see if we're trying to call a sub-command
            if (args.length != 0 && !nested.children.isEmpty()) {
                String subCmd = getSubCommand(nested, args);

                if (subCmd != null) {
                    // we have a subcommand to use!
                    AbstractCommand sub = nested.children.get(subCmd);

                    // adjust the arguments to match - BREAKING!!
                    int i = subCmd.indexOf(' ') == -1 ? 1 : 2;
                    String[] newArgs = new String[args.length - i];
                    System.arraycopy(args, i, newArgs, 0, newArgs.length);

                    // now process the command
                    processRequirements(sub, sender, newArgs);
                    return true;
                }
            }

            // if we've gotten this far, then just use the command we have
            if (nested.parent != null) {
                processRequirements(nested.parent, sender, args);
                return true;
            }
        }

        sender.sendMessage(this.msg_noCommand);
        return true;
    }

    private String getSubCommand(SimpleNestedCommand nested, String[] args) {
        String cmd = args[0].toLowerCase();
        if (nested.children.containsKey(cmd)) {
            return cmd;
        }

        String match = null;
        // support for multi-argument subcommands
        if (args.length >= 2 && nested.children.keySet().stream().anyMatch(k -> k.indexOf(' ') != -1)) {
            for (int len = args.length; len > 1; --len) {
                String cmd2 = String.join(" ", Arrays.copyOf(args, len)).toLowerCase();
                if (nested.children.containsKey(cmd2)) {
                    return cmd2;
                }
            }
        }

        // if we don't have a subcommand, should we search for one?
        if (this.allowLooseCommands) {
            // do a "closest match"
            int count = 0;
            for (String child : nested.children.keySet()) {
                if (child.startsWith(cmd)) {
                    match = child;
                    if (++count > 1) {
                        // there can only be one!
                        match = null;
                        break;
                    }
                }
            }
        }

        return match;
    }

    private void processRequirements(AbstractCommand command, CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && command.isNoConsole()) {
            sender.sendMessage(this.msg_noConsole);
            return;
        }

        if (command.getPermissionNode() == null || sender.hasPermission(command.getPermissionNode())) {
            AbstractCommand.ReturnType returnType = command.runCommand(sender, args);

            if (returnType == AbstractCommand.ReturnType.NEEDS_PLAYER) {
                sender.sendMessage(this.msg_noConsole);
                return;
            }

            if (returnType == AbstractCommand.ReturnType.SYNTAX_ERROR) {
                for (String s : this.msg_syntaxError) {
                    sender.sendMessage(s.replace("%syntax%", command.getSyntax()));
                }
            }

            return;
        }

        sender.sendMessage(this.msg_noPerms);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        // grab the specific command that's being called
        SimpleNestedCommand nested = this.commands.get(command.getName().toLowerCase());

        if (nested != null) {
            if (args.length == 0 || nested.children.isEmpty()) {
                return nested.parent != null ? nested.parent.onTab(sender, args) : null;
            }

            // check for each sub-command that they have access to
            final boolean op = sender.isOp();
            final boolean console = !(sender instanceof Player);
            if (args.length == 1) {
                // suggest sub-commands that this user has access to
                final String arg = args[0].toLowerCase();
                return nested.children.entrySet().stream()
                        .filter(e -> !console || !e.getValue().isNoConsole())
                        .filter(e -> e.getKey().startsWith(arg))
                        .filter(e -> op || e.getValue().getPermissionNode() == null || sender.hasPermission(e.getValue().getPermissionNode()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            }

            // more than one arg, let's check to see if we have a command here
            String subCmd = getSubCommand(nested, args);
            AbstractCommand sub;
            if (subCmd != null &&
                    (sub = nested.children.get(subCmd)) != null &&
                    (!console || !sub.isNoConsole()) &&
                    (op || sub.getPermissionNode() == null || sender.hasPermission(sub.getPermissionNode()))) {
                // adjust the arguments to match - BREAKING!!
                int i = subCmd.indexOf(' ') == -1 ? 1 : 2;
                String[] newArgs = new String[args.length - i];
                System.arraycopy(args, i, newArgs, 0, newArgs.length);

                // we're good to go!
                return fetchList(sub, newArgs, sender);
            }
        }

        return Collections.emptyList();
    }

    private List<String> fetchList(AbstractCommand abstractCommand, String[] args, CommandSender sender) {
        List<String> list = abstractCommand.onTab(sender, args);

        if (args.length != 0) {
            String str = args[args.length - 1];

            if (list != null && str != null && !str.isEmpty()) {
                try {
                    list.removeIf(s -> !s.toLowerCase().startsWith(str.toLowerCase()));
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }

        return list;
    }

    public static void registerCommandDynamically(Plugin plugin, String command, CommandExecutor executor, TabCompleter tabManager) {
        try {
            // Retrieve the SimpleCommandMap from the server
            Class<?> clazzCraftServer = Bukkit.getServer().getClass();
            Object craftServer = clazzCraftServer.cast(Bukkit.getServer());
            SimpleCommandMap commandMap = (SimpleCommandMap) craftServer.getClass()
                    .getDeclaredMethod("getCommandMap").invoke(craftServer);

            // Construct a new Command object
            Constructor<PluginCommand> constructorPluginCommand = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructorPluginCommand.setAccessible(true);
            PluginCommand commandObject = constructorPluginCommand.newInstance(command, plugin);

            // If we're on Paper 1.8, we need to register timings (spigot creates timings on init, paper creates it on register)
            // later versions of paper create timings if needed when the command is executed
            if (ServerProject.isServer(ServerProject.PAPER, ServerProject.TACO) && ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
                Class<?> clazz = Class.forName("co.aikar.timings.TimingsManager");
                Method method = clazz.getMethod("getCommandTiming", String.class, Command.class);
                Field field = PluginCommand.class.getField("timings");

                field.set(commandObject, method.invoke(null, plugin.getName().toLowerCase(), commandObject));
            }

            // Set command action
            commandObject.setExecutor(executor);

            // Set tab complete
            commandObject.setTabCompleter(tabManager);

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
