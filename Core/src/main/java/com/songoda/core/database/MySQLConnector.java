package com.songoda.core.database;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.configuration.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnector implements DatabaseConnector {
    private final Plugin plugin;
    private HikariDataSource hikari;
    private boolean initializedSuccessfully;

    public MySQLConnector(SongodaPlugin plugin) {
        this(plugin, plugin.getDatabaseConfig());
    }

    public MySQLConnector(Plugin plugin, Config databaseConfig) {
        this.plugin = plugin;

        String hostname = databaseConfig.getString("Connection Settings.Hostname");
        int port = databaseConfig.getInt("Connection Settings.Port");
        String database = databaseConfig.getString("Connection Settings.Database");
        String username = databaseConfig.getString("Connection Settings.Username");
        String password = databaseConfig.getString("Connection Settings.Password");
        boolean useSSL = databaseConfig.getBoolean("Connection Settings.Use SSL");
        int poolSize = databaseConfig.getInt("Connection Settings.Pool Size");

        plugin.getLogger().info("Connecting to " + hostname + " : " + port + " using MySQL");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useSSL=" + useSSL);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);

        try {
            this.hikari = new HikariDataSource(config);
            this.initializedSuccessfully = true;
        } catch (Exception ex) {
            this.initializedSuccessfully = false;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.initializedSuccessfully;
    }

    @Override
    public void closeConnection() {
        this.hikari.close();
    }

    @Override
    public void connect(ConnectionCallback callback) {
        try (Connection connection = this.hikari.getConnection()) {
            callback.accept(connection);
        } catch (SQLException ex) {
            this.plugin.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public OptionalResult connectOptional(ConnectionOptionalCallback callback) {
        try (Connection connection = getConnection()) {
            return callback.accept(connection);
        } catch (Exception ex) {
            SongodaCore.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
        }
        return OptionalResult.empty();
    }

    @Override
    public <T> T connectResult(ConnectResult<T> callback, T... defaultValue) {
        try (Connection connection = getConnection()) {
            T result = callback.accept(connection);
            return result != null ? result : defaultValue.length > 0 ? defaultValue[0] : null;
        } catch (Exception ex) {
            SongodaCore.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
            return defaultValue.length > 0 ? defaultValue[0] : null;
        }
    }

    @Override
    public void connectDSL(DSLContextCallback callback) {
        try (Connection connection = getConnection()) {
            callback.accept(DSL.using(connection, SQLDialect.MYSQL));
        } catch (Exception ex) {
            this.plugin.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public OptionalResult connectDSLOptional(DSLContextOptionalCallback callback) {
        try (Connection connection = getConnection()) {
            return callback.accept(DSL.using(connection, SQLDialect.MYSQL));
        } catch (Exception ex) {
            SongodaCore.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
        }
        return OptionalResult.empty();
    }

    @Override
    public <T> T connectDSLResult(DSLConnectResult<T> callback, T... defaultValue) {
        try (Connection connection = getConnection()) {
            T result = callback.accept(DSL.using(connection, SQLDialect.MYSQL));
            return result != null ? result : defaultValue.length > 0 ? defaultValue[0] : null;
        } catch (Exception ex) {
            SongodaCore.getLogger().severe("An error occurred executing a MySQL query: " + ex.getMessage());
            ex.printStackTrace();
            return defaultValue.length > 0 ? defaultValue[0] : null;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.hikari.getConnection();
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }
}
