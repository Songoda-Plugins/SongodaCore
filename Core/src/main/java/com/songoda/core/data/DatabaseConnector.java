package com.songoda.core.data;

import org.jooq.DSLContext;

import java.sql.SQLException;

public interface DatabaseConnector {
    /**
     * Checks if the connection to the database has been created
     *
     * @return true if the connection is created, otherwise false
     */
    boolean isInitialized();

    /**
     * Closes all open connections to the database
     */
    void closeConnection();

    /**
     * Executes a callback with a Connection passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     */
    default void connect(ConnectionCallback callback) {
        connect(true, callback);
    }

    void connect(boolean sqlThread, ConnectionCallback callback);

    /**
     * Wraps a connection in a callback which will automagically handle catching sql errors
     */
    interface ConnectionCallback {
        void accept(DSLContext ctx) throws SQLException;
    }
}
