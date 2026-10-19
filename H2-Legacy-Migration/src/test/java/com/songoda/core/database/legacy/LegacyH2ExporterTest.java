package com.songoda.core.database.legacy;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyH2ExporterTest {

    @Test
    public void exportsLegacyDatabaseAndLeavesItReadable() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-legacy-export");
        Path database = folder.resolve("legacy");
        Path script = folder.resolve("legacy.sql");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:" + database
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE stacked_blocks (id BIGINT PRIMARY KEY, amount BIGINT)");
                statement.executeUpdate("INSERT INTO stacked_blocks VALUES (17, 4)");
            }
        }

        assertTrue(LegacyH2Exporter.exportIfSupported(database, script, "sa", ""));
        String exportedSql = Files.readString(script);
        assertTrue(exportedSql.toLowerCase().contains("stacked_blocks"));
        assertTrue(exportedSql.contains("17"));
        assertTrue(Files.exists(folder.resolve("legacy.mv.db")));
    }

    @Test
    public void skipsACorruptDatabaseWithoutCreatingAnExport() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-not-database");
        Path database = folder.resolve("not-a-database");
        Path script = folder.resolve("not-a-database.sql");
        Files.writeString(folder.resolve("not-a-database.mv.db"), "not an H2 file");

        assertFalse(LegacyH2Exporter.exportIfSupported(database, script, "sa", ""));
        assertFalse(Files.exists(script));
    }
}
