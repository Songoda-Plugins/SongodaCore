package com.songoda.core.database;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class H2DatabaseUpgradeTest {

    @Test
    public void upgradesLegacyDatabaseAndKeepsOriginalBackup() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-database-upgrade");
        Path databaseBase = folder.resolve("legacy");
        Path oldDatabase = Path.of(databaseBase + ".mv.db");
        try (var fixture = getClass().getResourceAsStream("/database/legacy-h2-1.4.200.mv.db")) {
            if (fixture == null) {
                throw new AssertionError("Missing H2 1.4.200 migration fixture");
            }
            Files.copy(fixture, oldDatabase);
        }

        assertTrue(H2DatabaseUpgrade.upgradeIfLegacy(databaseBase, "sa", "", Logger.getLogger("test"), true,
                (source, converted, user, password) -> createConvertedDatabase(converted, user, password)));

        Path backup;
        try (var files = Files.list(folder)) {
            backup = files.filter(path -> path.getFileName().toString().contains("pre-v2"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected a backup of the original database"));
        }
        assertTrue(Files.size(backup) > 0);
        try (Connection connection = DriverManager.getConnection(databaseUrl(databaseBase), "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT amount FROM stacked_blocks WHERE id = 17")) {
            assertTrue(result.next());
            assertEquals(4, result.getInt("amount"));
        }
    }

    @Test
    public void skipsDatabaseAlreadyReadableByCurrentH2() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-current-database");
        Path databaseBase = folder.resolve("current");
        createConvertedDatabase(Path.of(databaseBase + ".mv.db"), "sa", "");
        AtomicBoolean converterCalled = new AtomicBoolean();

        assertFalse(H2DatabaseUpgrade.upgradeIfLegacy(databaseBase, "sa", "", Logger.getLogger("test"),
                (source, converted, user, password) -> converterCalled.set(true)));
        assertFalse(converterCalled.get());
    }

    @Test
    public void skipsReadableDatabaseWhileAutoServerLockFileExists() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-auto-server-database");
        Path databaseBase = folder.resolve("current");
        AtomicBoolean converterCalled = new AtomicBoolean();
        String url = databaseUrl(databaseBase) + ";AUTO_SERVER=TRUE";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE stacked_blocks (id BIGINT PRIMARY KEY, amount INT)");
            assertTrue(Files.exists(Path.of(databaseBase + ".lock.db")));

            assertFalse(H2DatabaseUpgrade.upgradeIfLegacy(databaseBase, "sa", "", Logger.getLogger("test"), true,
                    (source, converted, user, password) -> converterCalled.set(true)));
        }

        assertFalse(converterCalled.get());
    }

    @Test
    public void preservesOriginalWhenConvertedDatabaseFailsValidation() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-invalid-conversion");
        Path databaseBase = folder.resolve("legacy");
        Path oldDatabase = Path.of(databaseBase + ".mv.db");
        try (var fixture = getClass().getResourceAsStream("/database/legacy-h2-1.4.200.mv.db")) {
            if (fixture == null) {
                throw new AssertionError("Missing H2 1.4.200 migration fixture");
            }
            Files.copy(fixture, oldDatabase);
        }
        byte[] originalBytes = Files.readAllBytes(oldDatabase);

        try {
            H2DatabaseUpgrade.upgradeIfLegacy(databaseBase, "sa", "", Logger.getLogger("test"),
                    (source, converted, user, password) -> Files.writeString(converted, "invalid response"));
            fail("Expected validation to reject the returned file");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("validation"));
        }

        assertTrue(java.util.Arrays.equals(originalBytes, Files.readAllBytes(oldDatabase)));
        try (var files = Files.list(folder)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().contains("pre-v2")));
        }
    }

    private static void createConvertedDatabase(Path convertedFile, String username, String password)
            throws java.io.IOException {
        String name = convertedFile.getFileName().toString();
        Path basePath = convertedFile.resolveSibling(name.substring(0, name.length() - ".mv.db".length()));
        try (Connection connection = DriverManager.getConnection(databaseUrl(basePath), username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE stacked_blocks (id BIGINT PRIMARY KEY, amount INT)");
            statement.execute("INSERT INTO stacked_blocks VALUES (17, 4)");
        } catch (java.sql.SQLException exception) {
            throw new java.io.IOException("Could not create converted database fixture", exception);
        }
    }

    private static String databaseUrl(Path basePath) {
        return "jdbc:h2:file:" + basePath.toAbsolutePath().normalize()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
    }
}
