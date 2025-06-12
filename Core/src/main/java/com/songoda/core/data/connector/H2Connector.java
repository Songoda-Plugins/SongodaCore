package com.songoda.core.data.connector;

import com.songoda.core.data.DatabaseConnector;
import com.songoda.core.data.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;

public class H2Connector implements DatabaseConnector {
    private HikariDataSource hikari;
    private boolean initializedSuccessfully;

    public H2Connector(String databaseFile, int poolSize) {
        System.out.println("Connecting to H2 database: " + databaseFile);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com;songoda;third_party;org;h2;Driver".replace(";", "."));
        config.setJdbcUrl("jdbc:h2:" + databaseFile + ";AUTO_RECONNECT=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
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
    public void connect(boolean sqlThread, ConnectionCallback callback) {
        Runnable runnable = () -> {
            try (Connection connection = this.hikari.getConnection()) {
                callback.accept(DSL.using(connection, SQLDialect.MYSQL));
            } catch (Exception ex) {
                System.out.println("An error occurred executing an H2 query: " + ex.getMessage());
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
