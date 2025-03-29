package com.songoda.core.data;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.configuration.Config;
import com.songoda.core.data.connector.H2Connector;
import com.songoda.core.data.connector.MariaDBConnector;
import com.songoda.core.data.connector.MySQLConnector;
import com.songoda.core.data.connector.SQLiteConnector;
import com.songoda.core.thread.MonitoredThread;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private static DatabaseManager INSTANCE;

    private final MonitoredThread thread;
    private final DatabaseConnector connector;

    private final Config databaseConfig;

    public DatabaseManager(SongodaPlugin plugin) {
        INSTANCE = this;
        this.thread = new MonitoredThread(plugin.getName().toLowerCase() + "-sql-thread", 15, TimeUnit.SECONDS);
        this.databaseConfig = new Config(plugin, "database.yml");

        if (!new File(plugin.getDataFolder(), "database.yml").exists())
            plugin.saveResource("database.yml", false);
        this.databaseConfig.load();

        String type = this.databaseConfig.getString("type", "H2").toUpperCase();
        String host = this.databaseConfig.getString("host", "localhost");
        int port = this.databaseConfig.getInt("port", 3306);
        String database = this.databaseConfig.getString("database", "plugin");
        String username = this.databaseConfig.getString("username", "root");
        String password = this.databaseConfig.getString("password", "");
        int poolSize = this.databaseConfig.getInt("poolSize", 10);
        boolean useSSL = this.databaseConfig.getBoolean("useSSL", false);
        boolean autoReconnect = this.databaseConfig.getBoolean("autoReconnect", true);

        String dataPath = plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + "/";

        String dbFile = "./" + dataPath + this.databaseConfig.getString("file", "data");

        switch (DatabaseType.valueOf(type)) {
            case H2:
                this.connector = new H2Connector(dbFile, poolSize);
                break;
            case MYSQL:
                this.connector = new MySQLConnector(host, port, database, username, password, useSSL, autoReconnect, poolSize);
                break;
            case SQLITE:
                this.connector = new SQLiteConnector(dbFile, poolSize);
                break;
            case MARIADB:
                this.connector = new MariaDBConnector(host, port, database, username, password, useSSL, autoReconnect, poolSize);
                break;
            default:
                throw new IllegalArgumentException("Invalid database type: " + type);
        }
    }

    public void execute(Runnable runnable) {
        execute(runnable, false);
    }

    public void execute(Runnable runnable, boolean nonDisruptable) {
        this.thread.execute(runnable, nonDisruptable);
    }

    public void load(String name, Runnable load) {
        load.run();
        System.out.println("Loaded " + name);
    }

    public DatabaseConnector getDatabaseConnector() {
        return this.connector;
    }

    public Config getConfig() {
        return this.databaseConfig;
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }
}
