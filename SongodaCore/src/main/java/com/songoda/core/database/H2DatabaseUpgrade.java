package com.songoda.core.database;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Safely upgrades file-based H2 databases created by H2 1.4 before the current driver opens them.
 */
public final class H2DatabaseUpgrade {

    private static final DateTimeFormatter BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private H2DatabaseUpgrade() {
    }

    /**
     * Sends a legacy H2 database to Songoda's hosted converter, validates the returned file, and preserves a backup
     * before replacing the original database.
     *
     * @param databaseBasePath path passed to H2 without the {@code .mv.db} suffix
     * @param username H2 username
     * @param password H2 password
     * @param logger logger used to report successful conversion
     * @return {@code true} when a legacy database was converted
     * @throws IOException if conversion fails; the original database remains untouched
     */
    public static boolean upgradeIfLegacy(Path databaseBasePath, String username, String password,
                                          Logger logger) throws IOException {
        return upgradeIfLegacy(databaseBasePath, username, password, logger, true);
    }

    /**
     * Sends a legacy H2 database to Songoda's hosted converter when it cannot be opened using the configured H2 mode.
     *
     * @param databaseBasePath path passed to H2 without the {@code .mv.db} suffix
     * @param username H2 username
     * @param password H2 password
     * @param logger logger used to report conversion progress
     * @param h2ServerModeEnabled whether the H2 connection uses {@code AUTO_SERVER}
     * @return {@code true} when a legacy database was converted
     * @throws IOException if conversion fails; the original database remains untouched
     */
    public static boolean upgradeIfLegacy(Path databaseBasePath, String username, String password,
                                          Logger logger, boolean h2ServerModeEnabled) throws IOException {
        H2DatabaseConversionClient client = new H2DatabaseConversionClient();
        return upgradeIfLegacy(databaseBasePath, username, password, logger, h2ServerModeEnabled,
                client::convert);
    }

    static boolean upgradeIfLegacy(Path databaseBasePath, String username, String password, Logger logger,
                                   LegacyDatabaseConverter converter) throws IOException {
        return upgradeIfLegacy(databaseBasePath, username, password, logger, true, converter);
    }

    static boolean upgradeIfLegacy(Path databaseBasePath, String username, String password, Logger logger,
                                   boolean h2ServerModeEnabled, LegacyDatabaseConverter converter) throws IOException {
        Path basePath = databaseBasePath.toAbsolutePath().normalize();
        Path databaseFile = Path.of(basePath + ".mv.db");
        if (!Files.isRegularFile(databaseFile)) {
            return false;
        }
        if (isReadableByCurrentH2(basePath, username, password, h2ServerModeEnabled)) {
            return false;
        }
        if (Files.exists(Path.of(basePath + ".lock.db"))) {
            throw new IOException("The legacy H2 database is still locked and cannot be read by the installed driver; "
                    + "stop any process using it and retry.");
        }

        logger.info("Converting H2 database...");
        Path workingDirectory = Files.createTempDirectory(databaseFile.getParent(), "h2-legacy-upgrade-");
        Path convertedFile = workingDirectory.resolve("converted-database.mv.db");
        try {
            try {
                converter.convert(databaseFile, convertedFile, username, password);
            } catch (IOException exception) {
                logger.severe("H2 database conversion failed.");
                throw new H2ConversionException("Legacy H2 conversion failed.", exception);
            }
            if (!Files.isRegularFile(convertedFile)) {
                throw new H2ConversionException("Songoda's H2 conversion service did not return a database file.");
            }

            try {
                validateConvertedDatabase(workingDirectory.resolve("converted-database"), username, password,
                        h2ServerModeEnabled);
            } catch (SQLException exception) {
                logger.warning("H2 database conversion failed validation.");
                throw new H2ConversionException("The converted H2 database failed validation.", exception);
            }
            Path backupFile = uniqueBackupPath(databaseFile);
            Files.copy(databaseFile, backupFile);
            replaceDatabase(convertedFile, databaseFile);
            logger.info("H2 database conversion succeeded.");
            return true;
        } finally {
            try {
                deleteWorkingDirectory(workingDirectory);
            } catch (IOException exception) {
                logger.warning("Could not remove temporary files used for the H2 upgrade at " + workingDirectory
                        + ": " + exception.getMessage());
            }
        }
    }

    @FunctionalInterface
    interface LegacyDatabaseConverter {
        void convert(Path legacyDatabaseFile, Path convertedDatabaseFile, String username, String password)
                throws IOException;
    }

    public static final class H2ConversionException extends IOException {
        public H2ConversionException(String message) {
            super(message);
        }

        public H2ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static boolean isReadableByCurrentH2(Path databaseBasePath, String username, String password,
                                                 boolean h2ServerModeEnabled) {
        String url = h2FileUrl(databaseBasePath, h2ServerModeEnabled) + ";IFEXISTS=TRUE";
        try (Connection connection = openConnection(url, username, password)) {
            // H2 can defer reading the database catalog until metadata is requested. A successful
            // connection alone therefore does not prove that the file format is supported.
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String schema = tables.getString("TABLE_SCHEM");
                    String table = tables.getString("TABLE_NAME");
                    try (ResultSet columns = metadata.getColumns(null, schema, table, "%")) {
                        while (columns.next()) {
                            columns.getString("COLUMN_NAME");
                        }
                    }
                }
            }
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private static void validateConvertedDatabase(Path databaseBasePath, String username, String password,
                                                  boolean h2ServerModeEnabled) throws SQLException {
        String url = h2FileUrl(databaseBasePath, h2ServerModeEnabled) + ";IFEXISTS=TRUE";
        try (Connection ignored = openConnection(url, username, password)) {
            // Opening the response with the installed H2 driver confirms its file format and credentials.
        }
    }

    private static String h2FileUrl(Path databaseBasePath, boolean h2ServerModeEnabled) {
        String url = "jdbc:h2:file:" + databaseBasePath.toAbsolutePath().normalize();
        if (h2ServerModeEnabled) {
            url += ";AUTO_SERVER=TRUE";
        }
        return url + ";AUTO_RECONNECT=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
    }

    private static Connection openConnection(String url, String username, String password) throws SQLException {
        try {
            Class<?> driverClass = Class.forName("org.h2.Driver", true, H2DatabaseUpgrade.class.getClassLoader());
            Driver driver = (Driver) driverClass.getConstructor().newInstance();
            Properties credentials = new Properties();
            credentials.setProperty("user", username == null ? "sa" : username);
            credentials.setProperty("password", password == null ? "" : password);
            Connection connection = driver.connect(url, credentials);
            if (connection == null) {
                throw new SQLException("The installed H2 driver does not accept this JDBC URL.");
            }
            return connection;
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new SQLException("Could not load the installed H2 driver from the plugin class loader.", exception);
        }
    }

    private static Path uniqueBackupPath(Path databaseFile) {
        String timestamp = BACKUP_SUFFIX.format(Instant.now());
        Path candidate = databaseFile.resolveSibling(databaseFile.getFileName() + ".pre-v2-" + timestamp + ".bak");
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = databaseFile.resolveSibling(databaseFile.getFileName() + ".pre-v2-" + timestamp + "-" + suffix++ + ".bak");
        }
        return candidate;
    }

    private static void replaceDatabase(Path convertedFile, Path databaseFile) throws IOException {
        try {
            Files.move(convertedFile, databaseFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(convertedFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteWorkingDirectory(Path workingDirectory) throws IOException {
        if (!Files.exists(workingDirectory)) {
            return;
        }
        try (var paths = Files.walk(workingDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
