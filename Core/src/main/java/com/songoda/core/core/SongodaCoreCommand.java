package com.songoda.core.core;

import com.songoda.core.SongodaCore;
import com.songoda.core.commands.AbstractCommand;
import com.songoda.core.gui.GuiManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SongodaCoreCommand extends AbstractCommand {
    protected GuiManager guiManager;

    public SongodaCoreCommand() {
        super(CommandType.CONSOLE_OK, "songoda");
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (sender instanceof Player) {
            if (this.guiManager == null || this.guiManager.isClosed()) {
                this.guiManager = new GuiManager(SongodaCore.getHijackedPlugin());
            }

            this.guiManager.showGUI((Player) sender, new SongodaCoreOverviewGUI());
        } else {
            sender.sendMessage("/songoda diag");
        }

        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return "songoda.admin";
    }

    @Override
    public String getSyntax() {
        return "/songoda";
    }

    @Override
    public String getDescription() {
        return "Displays this interface.";
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }
}
