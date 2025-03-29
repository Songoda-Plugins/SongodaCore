package com.songoda.core.commands;

import com.songoda.core.SongodaCore;
import com.songoda.core.chat.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand extends AbstractCommand {
    String header = null;
    String description;
    boolean sortHelp = false;
    final String command;
    final Plugin plugin;
    protected final SimpleNestedCommand nestedCommands;

    public MainCommand(Plugin plugin, String command) {
        super(CommandType.CONSOLE_OK, command);

        this.command = command;
        this.plugin = plugin;
        this.description = "Shows the command help page for /" + command;
        this.nestedCommands = new SimpleNestedCommand(this);
    }

    public MainCommand setHeader(String header) {
        this.header = header;
        return this;
    }

    public MainCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public MainCommand setSortHelp(boolean sortHelp) {
        this.sortHelp = sortHelp;
        return this;
    }

    public MainCommand addSubCommand(AbstractCommand command) {
        this.nestedCommands.addSubCommand(command);
        return this;
    }

    public MainCommand addSubCommands(AbstractCommand... commands) {
        this.nestedCommands.addSubCommands(commands);
        return this;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        sender.sendMessage("");

        if (this.header != null) {
            sender.sendMessage(this.header);
        } else {
            AdventureUtils.sendMessage(SongodaCore.getHijackedPlugin(), AdventureUtils.formatComponent(
                    String.format("<color:#ff8080>&l%s &8» &7Version %s Created with <3 by <b><i><gradient:#ec4e74:#f4c009>Songoda</gradient>",
                            this.plugin.getDescription().getName(),
                            this.plugin.getDescription().getVersion()
                    )), sender);
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + "/songoda" + ChatColor.GRAY + " - Opens the Songoda plugin GUI");
        sender.sendMessage("");

        if (this.nestedCommands != null) {
            List<String> commands = this.nestedCommands.children.values().stream().distinct().map(c -> c.getCommands().get(0)).collect(Collectors.toList());

            if (this.sortHelp) {
                Collections.sort(commands);
            }

            boolean isPlayer = sender instanceof Player;
            // todo? pagination if commands.size is too large? (player-only)
            sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + getSyntax() + ChatColor.GRAY + " - " + getDescription());

            for (String cmdStr : commands) {
                final AbstractCommand cmd = this.nestedCommands.children.get(cmdStr);
                if (cmd == null) continue;
                if (!isPlayer) {
                    sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription());
                } else if (cmd.getPermissionNode() == null || sender.hasPermission(cmd.getPermissionNode())) {
                    String command = "/" + this.command + " ";
                    Component component = AdventureUtils.formatComponent(
                            String.format("<DARK_GRAY>- <YELLOW>%s%s <GRAY>- %s %s",
                                    command,
                                    cmd.getSyntax(),
                                    cmd.getDescription(),
                                    cmd.getPermissionNode() == null ? "" : cmd.getPermissionNode()
                            )
                    );
                    AdventureUtils.sendMessage(SongodaCore.getHijackedPlugin(), component, sender);
                }
            }
        }

        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        // don't need to worry about tab for a root command - handled by the manager
        return null;
    }

    @Override
    public String getPermissionNode() {
        // permissions for a root command should be handled in the plugin.yml
        return null;
    }

    @Override
    public String getSyntax() {
        return "/" + this.command;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
