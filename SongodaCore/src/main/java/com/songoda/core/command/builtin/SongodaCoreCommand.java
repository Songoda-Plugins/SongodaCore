package com.songoda.core.command.builtin;

import com.songoda.core.command.annotation.BaseCommand;
import com.songoda.core.command.annotation.Sender;
import com.songoda.core.command.annotation.SubCommand;
import org.bukkit.command.CommandSender;

//@Command("songodacore")
//@Permission("songodacore.admin")
public class SongodaCoreCommand {

    @BaseCommand
    public void onBaseCommand(@Sender CommandSender sender) {
        // This method is called when the base command is executed
        // You can add your logic here
    }

    @SubCommand("database export")
    public void onExportCommand(@Sender CommandSender sender) {
        //TODO make database export/import
    }

    @SubCommand("database import")
    public void onImportCommand(@Sender CommandSender sender) {
        //TODO make database export/import
    }
}
