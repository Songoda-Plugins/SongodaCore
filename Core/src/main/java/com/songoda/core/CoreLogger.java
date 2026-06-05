package com.songoda.core;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class CoreLogger extends Logger {
    private static final CoreLogger INSTANCE = new CoreLogger();

    private final String corePrefix = "[" + SongodaCoreConstants.getProjectName() + "] ";
    private String pluginPrefix = "";

    private CoreLogger() {
        super(CoreLogger.class.getCanonicalName(), null);
        setLevel(Level.ALL);

        LogManager.getLogManager().addLogger(this);
    }

    void setPlugin(Plugin plugin) {
        this.pluginPrefix = "[" + plugin.getName() + "] ";
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(this.pluginPrefix + this.corePrefix + record.getMessage());
        super.log(record);
    }

    public static CoreLogger getInstance() {
        return INSTANCE;
    }
}
