package com.songoda.core.data.connector;

import com.songoda.core.data.DatabaseConnector;
import com.songoda.core.data.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;

public class MariaDBConnector implements DatabaseConnector {
    private HikariDataSource hikari;
    private boolean initializedSuccessfully;

    public MariaDBConnector(String hostname, int port, String database, String username, String password, boolean useSSL, boolean autoReconnect, int poolSize) {
        System.out.println("Connecting to MariaDB: " + hostname + ":" + port);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + hostname + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.addDataSourceProperty("useSSL", useSSL);
        config.addDataSourceProperty("autoReconnect", autoReconnect);

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
    public void connect(boolean sqlThread, ConnectionCallback callback) {
        Runnable runnable = () -> {
            try (Connection connection = this.hikari.getConnection()) {
                callback.accept(DSL.using(connection, SQLDialect.MYSQL));
            } catch (Exception ex) {
                System.out.println("An error occurred executing a MariaDB query: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        if (sqlThread) {
            DatabaseManager.getInstance().execute(runnable);
        } else {
            runnable.run();
        }
    }
}
