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

public class H2Connector implements DatabaseConnector {
    private final Plugin plugin;
    private HikariDataSource hikari;
    private boolean initializedSuccessfully;

    H2Connector() {
        this.plugin = null;

        int poolSize = 2;
        String username = "username";
        String password = "password";

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:./db_test/CraftaroCoreTest;AUTO_RECONNECT=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);

        try {
            this.hikari = new HikariDataSource(config);
            this.initializedSuccessfully = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            this.initializedSuccessfully = false;
        }
    }

    public H2Connector(SongodaPlugin plugin) {
        this(plugin, plugin.getDatabaseConfig());
    }

    public H2Connector(Plugin plugin, Config databaseConfig) {
        this.plugin = plugin;

        int poolSize = databaseConfig.getInt("Connection Settings.Pool Size");
        String username = databaseConfig.getString("Connection Settings.Username");
        String password = databaseConfig.getString("Connection Settings.Password");

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com;songoda;third_party;org;h2;Driver".replace(";", "."));
        config.setJdbcUrl("jdbc:h2:./" + plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + "/" + plugin.getDescription().getName().toLowerCase() + ";AUTO_RECONNECT=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);

        try {
            this.hikari = new HikariDataSource(config);
            this.initializedSuccessfully = true;
        } catch (Exception ex) {
            ex.printStackTrace();
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
            this.plugin.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public OptionalResult connectOptional(ConnectionOptionalCallback callback) {
        try (Connection connection = getConnection()) {
            return callback.accept(connection);
        } catch (Exception ex) {
            if (this.plugin != null) {
                SongodaCore.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            }
            ex.printStackTrace();
        }
        return OptionalResult.empty();
    }

    @Override
    public <T> T connectResult(ConnectResult<T> callback, T... defaultValue) {
        try (Connection connection = getConnection()) {
            T result =  callback.accept(connection);
            return result != null ? result : defaultValue.length > 0 ? defaultValue[0] : null;
        } catch (Exception ex) {
            if (this.plugin != null) {
                SongodaCore.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            }
            ex.printStackTrace();
            return defaultValue.length > 0 ? defaultValue[0] : null;
        }
    }

    @Override
    public void connectDSL(DSLContextCallback callback) {
        try (Connection connection = getConnection()) {
            callback.accept(DSL.using(connection, SQLDialect.MYSQL));
        } catch (Exception ex) {
            if (this.plugin != null) {
                this.plugin.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }

    @Override
    public OptionalResult connectDSLOptional(DSLContextOptionalCallback callback) {
        try (Connection connection = getConnection()) {
            return callback.accept(DSL.using(connection, SQLDialect.MYSQL));
        } catch (Exception ex) {
            if (this.plugin != null) {
                SongodaCore.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            }
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
            if (this.plugin != null) {
                SongodaCore.getLogger().severe("An error occurred executing a H2 query: " + ex.getMessage());
            }
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
        return DatabaseType.H2;
    }
}
