package com.songoda.core.core;

import com.songoda.core.SongodaCore;
import com.songoda.core.commands.AbstractCommand;
import com.songoda.core.compatibility.ClassMapping;
import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.List;

public class SongodaCoreDiagCommand extends AbstractCommand {
    private final DecimalFormat decimalFormat = new DecimalFormat("##.##");

    private Object nmsServerInstance;
    private Field recentTpsOnNmsServer;

    public SongodaCoreDiagCommand() {
        super(CommandType.CONSOLE_OK, "diag");

        try {
            this.nmsServerInstance = ClassMapping.MINECRAFT_SERVER.getClazz().getMethod("getServer").invoke(null);
            this.recentTpsOnNmsServer = this.nmsServerInstance.getClass().getField("recentTps");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getPermissionNode() {
        return "songoda.admin";
    }

    @Override
    public String getSyntax() {
        return "/songoda diag";
    }

    @Override
    public String getDescription() {
        return "Display diagnostics information.";
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        sender.sendMessage("");
        sender.sendMessage("Songoda Diagnostics Information");
        sender.sendMessage("");
        sender.sendMessage("Plugins:");

        for (PluginInfo plugin : SongodaCore.getPlugins()) {
            sender.sendMessage(String.format(
                    "%s v%s (Core v%s) (Dependencies v%s)",
                    plugin.getJavaPlugin().getName(),
                    plugin.getJavaPlugin().getDescription().getVersion(),
                    plugin.getCoreLibraryVersion(),
                    plugin.getDependencyVersion()
            ));
        }

        sender.sendMessage("");
        sender.sendMessage("Server Version: " + Bukkit.getVersion());
        sender.sendMessage("NMS: " + ServerProject.getServerVersion() + " " + ServerVersion.getServerVersionString());
        sender.sendMessage("Operating System: " + System.getProperty("os.name"));
        sender.sendMessage("Allocated Memory: " + getRuntimeMaxMemory());
        sender.sendMessage("Online Players: " + Bukkit.getOnlinePlayers().size());
        sendCurrentTps(sender);
        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    private String getRuntimeMaxMemory() {
        return this.decimalFormat.format(Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MiB";
    }

    private void sendCurrentTps(CommandSender receiver) {
        if (this.recentTpsOnNmsServer == null) {
            return;
        }

        try {
            double[] tps = ((double[]) this.recentTpsOnNmsServer.get(this.nmsServerInstance));

            receiver.sendMessage(String.format("TPS from last 1m, 5m, 15m: %s, %s, %s", this.decimalFormat.format(tps[0]), this.decimalFormat.format(tps[1]), this.decimalFormat.format(tps[2])));
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }
}
