package com.songoda.core.database;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PluginDataBackupTest {

    @Test
    public void archivesAllExistingPluginFilesBeforeFirstMigration() throws Exception {
        Path root = Files.createTempDirectory("songoda-plugin-data-backup");
        Path dataDirectory = Files.createDirectories(root.resolve("plugins/ExamplePlugin"));
        Path backupDirectory = root.resolve("plugins/.songoda-backups");
        Path pluginJar = Files.writeString(root.resolve("ExamplePlugin.jar"), "plugin artifact");
        byte[] database = new byte[]{1, 2, 3, 4, 5};
        Files.writeString(dataDirectory.resolve("config.yml"), "setting: true\n");
        Files.write(dataDirectory.resolve("example.mv.db"), database);
        Files.createDirectories(dataDirectory.resolve("custom/empty"));

        Path backup = PluginDataBackup.backupIfChanged(dataDirectory, backupDirectory,
                "ExamplePlugin", "5.0.0", pluginJar);

        assertNotNull(backup);
        assertTrue(Files.isRegularFile(backup));
        assertTrue(backup.startsWith(backupDirectory));
        try (ZipFile zip = new ZipFile(backup.toFile())) {
            assertNotNull(zip.getEntry("config.yml"));
            assertNotNull(zip.getEntry("example.mv.db"));
            assertNotNull(zip.getEntry("custom/empty/"));
            try (InputStream input = zip.getInputStream(zip.getEntry("example.mv.db"))) {
                assertArrayEquals(database, input.readAllBytes());
            }
        }
    }

    @Test
    public void skipsBackupWhenVersionIsUnchangedAndCreatesAnotherAfterUpdate() throws Exception {
        Path root = Files.createTempDirectory("songoda-plugin-data-version");
        Path dataDirectory = Files.createDirectories(root.resolve("plugins/ExamplePlugin"));
        Path backupDirectory = root.resolve("plugins/.songoda-backups");
        Path pluginJar = Files.writeString(root.resolve("ExamplePlugin.jar"), "plugin artifact 1");
        Files.writeString(dataDirectory.resolve("config.yml"), "setting: true\n");

        Path firstBackup = PluginDataBackup.backupIfChanged(dataDirectory, backupDirectory,
                "ExamplePlugin", "5.0.0", pluginJar);
        assertNotNull(firstBackup);
        PluginDataBackup.markVersion(dataDirectory, "5.0.0", pluginJar);

        assertNull(PluginDataBackup.backupIfChanged(dataDirectory, backupDirectory,
                "ExamplePlugin", "5.0.0", pluginJar));

        Files.writeString(dataDirectory.resolve("config.yml"), "setting: false\n");
        Path secondBackup = PluginDataBackup.backupIfChanged(dataDirectory, backupDirectory,
                "ExamplePlugin", "5.1.0", pluginJar);

        assertNotNull(secondBackup);
        assertTrue(Files.exists(secondBackup));
        try (ZipFile zip = new ZipFile(secondBackup.toFile())) {
            try (InputStream version = zip.getInputStream(zip.getEntry(".songoda-plugin-version"));
                 InputStream config = zip.getInputStream(zip.getEntry("config.yml"))) {
                assertEquals("version=5.0.0" + System.lineSeparator()
                        + "sha256=" + sha256(pluginJar), new String(version.readAllBytes(), StandardCharsets.UTF_8));
                assertEquals("setting: false\n", new String(config.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    public void createsBackupWhenSnapshotArtifactChangesWithoutVersionChange() throws Exception {
        Path root = Files.createTempDirectory("songoda-plugin-data-snapshot");
        Path dataDirectory = Files.createDirectories(root.resolve("plugins/ExamplePlugin"));
        Path backupDirectory = root.resolve("plugins/.songoda-backups");
        Path pluginJar = Files.writeString(root.resolve("ExamplePlugin.jar"), "first snapshot");
        Files.writeString(dataDirectory.resolve("config.yml"), "setting: true\n");

        PluginDataBackup.markVersion(dataDirectory, "5.0.0-SNAPSHOT", pluginJar);
        Files.writeString(pluginJar, "updated snapshot");

        Path backup = PluginDataBackup.backupIfChanged(dataDirectory, backupDirectory,
                "ExamplePlugin", "5.0.0-SNAPSHOT", pluginJar);

        assertNotNull(backup);
        assertTrue(Files.exists(backup));
    }

    @Test
    public void doesNotCreateArchiveForAnEmptyNewInstall() throws Exception {
        Path root = Files.createTempDirectory("songoda-plugin-data-new");
        Path dataDirectory = Files.createDirectories(root.resolve("plugins/ExamplePlugin"));

        Path pluginJar = Files.writeString(root.resolve("ExamplePlugin.jar"), "plugin artifact");
        assertNull(PluginDataBackup.backupIfChanged(dataDirectory, root.resolve("plugins/.songoda-backups"),
                "ExamplePlugin", "5.0.0", pluginJar));
    }

    private String sha256(Path file) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return java.util.HexFormat.of().formatHex(digest);
    }
}
