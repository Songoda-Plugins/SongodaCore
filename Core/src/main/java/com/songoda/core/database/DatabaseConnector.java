package com.songoda.core.database;

import org.jooq.DSLContext;

import java.sql.Connection;
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
    void connect(ConnectionCallback callback);

    /**
     * Executes a callback with a Connection passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     *
     * @return The result of the callback
     */
    OptionalResult connectOptional(ConnectionOptionalCallback callback);

    /**
     * Executes a callback with a Connection passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     * @param defaultValue The default value (First value only) to return if the callback fails (Also used to make automatic type inference work)
     *
     * @return The result of the callback
     */
    <T> T connectResult(ConnectResult<T> callback, T... defaultValue);

    /**
     * Executes a callback with a DSLContext passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     */
    void connectDSL(DSLContextCallback callback);

    /**
     * Executes a callback with a DSLContext passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     *
     * @return The result of the callback
     */
    OptionalResult connectDSLOptional(DSLContextOptionalCallback callback);

    /**
     * Executes a callback with a DSLContext passed and automatically closes it when finished
     *
     * @param callback The callback to execute once the connection is retrieved
     * @param defaultValue The default value (First value only) to return if the callback fails (Also used to make automatic type inference work)
     *
     * @return The result of the callback
     */
    <T> T connectDSLResult(DSLConnectResult<T> callback, T... defaultValue);

    /**
     * Wraps a connection in a callback which will automagically handle catching sql errors
     */
    interface ConnectionCallback {
        void accept(Connection connection) throws SQLException;
    }

    /**
     * Wraps a connection in a callback which will
     * automagically handle catching sql errors
     * Can return a value
     */
    interface ConnectionOptionalCallback {
        OptionalResult accept(Connection connection) throws SQLException;
    }

    /**
     * Wraps a connection in a callback which will automagically handle catching sql errors
     */
    interface DSLContextCallback {
        void accept(DSLContext context) throws SQLException;
    }

    /**
     * Wraps a connection in a callback which will
     * automagically handle catching sql errors
     * Can return a value
     */
    interface DSLContextOptionalCallback {
        OptionalResult accept(DSLContext context) throws SQLException;
    }

    /**
     * Wraps a connection in a callback which will
     * automagically handle catching sql errors
     * Can return a value
     */
    interface ConnectResult<T> {
        T accept(Connection connection) throws SQLException;
    }

    interface DSLConnectResult<T> {
        T accept(DSLContext context) throws SQLException;
    }

    /**
     * Gets a connection from the database
     *
     * @return The connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Gets the database type
     *
     * @return The database type
     */
    DatabaseType getType();
}
