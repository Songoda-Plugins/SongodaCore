package com.songoda.core.database.legacy;

import org.h2.Driver;
import org.h2.tools.Script;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Exports a legacy H2 1.4 database to SQL without modifying its database file.
 */
public final class LegacyH2Exporter {

    private LegacyH2Exporter() {
    }

    /**
     * Exports {@code databaseBasePath} when it contains a format readable by H2 1.4.200.
     *
     * @return {@code true} when the database was exported, or {@code false} when the file is
     *         not an H2 1.4 database that this exporter can read
     * @throws SQLException when the legacy database is recognized but cannot be safely exported
     */
    public static boolean exportIfSupported(Path databaseBasePath, Path scriptPath, String username,
                                            String password) throws SQLException {
        String url = "jdbc:h2:file:" + databaseBasePath.toAbsolutePath().normalize()
                + ";IFEXISTS=TRUE;ACCESS_MODE_DATA=r;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        Properties credentials = new Properties();
        credentials.setProperty("user", username);
        credentials.setProperty("password", password);

        Connection connection;
        try {
            connection = new Driver().connect(url, credentials);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 90048 || exception.getErrorCode() == 90028) {
                return false;
            }
            throw exception;
        }
        if (connection == null) {
            return false;
        }

        try (connection) {
            Script.process(connection, scriptPath.toAbsolutePath().normalize().toString(), "", "");
        }
        return true;
    }
}
