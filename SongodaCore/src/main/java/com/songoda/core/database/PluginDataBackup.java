package com.songoda.core.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Creates a complete, versioned backup of a plugin data folder before its update migrations run. */
public final class PluginDataBackup {

    private static final String VERSION_MARKER = ".songoda-plugin-version";
    private static final DateTimeFormatter BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private PluginDataBackup() {
    }

    /**
     * Archives the data directory when the installed plugin artifact differs from the last successful startup.
     *
     * @param dataDirectory plugin data directory
     * @param backupDirectory directory outside the plugin data directory for backup archives
     * @param pluginName plugin display name
     * @param currentVersion installed plugin version
     * @param pluginArtifact installed plugin jar
     * @return the created archive, or {@code null} when no backup was needed
     * @throws IOException if the directory cannot be completely archived and verified
     */
    public static Path backupIfChanged(Path dataDirectory, Path backupDirectory, String pluginName,
                                       String currentVersion, Path pluginArtifact) throws IOException {
        Path normalizedDataDirectory = dataDirectory.toAbsolutePath().normalize();
        Path marker = normalizedDataDirectory.resolve(VERSION_MARKER);
        PluginVersion previous = readPluginVersion(marker);
        String artifactHash = sha256(pluginArtifact);
        if ((previous != null && currentVersion.equals(previous.version())
                && artifactHash.equals(previous.sha256())) || !containsData(normalizedDataDirectory, marker)) {
            return null;
        }

        Path normalizedBackupDirectory = backupDirectory.toAbsolutePath().normalize();
        if (normalizedBackupDirectory.startsWith(normalizedDataDirectory)) {
            throw new IOException("The backup directory must be outside the plugin data directory.");
        }
        Files.createDirectories(normalizedBackupDirectory);

        String versionLabel = previous == null ? "legacy" : sanitize(previous.version());
        String archiveName = sanitize(pluginName) + "-" + versionLabel + "-to-" + sanitize(currentVersion)
                + "-" + BACKUP_SUFFIX.format(Instant.now()) + ".zip";
        Path archive = uniqueArchivePath(normalizedBackupDirectory.resolve(archiveName));
        Path temporaryArchive = Files.createTempFile(normalizedBackupDirectory, archiveName, ".tmp");
        try {
            writeArchive(normalizedDataDirectory, temporaryArchive);
            verifyArchive(temporaryArchive);
            move(temporaryArchive, archive);
            return archive;
        } catch (IOException exception) {
            Files.deleteIfExists(temporaryArchive);
            throw exception;
        }
    }

    /** Writes the current version marker after the plugin has completed startup successfully. */
    public static void markVersion(Path dataDirectory, String currentVersion, Path pluginArtifact) throws IOException {
        Path normalizedDataDirectory = dataDirectory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDataDirectory);
        Path marker = normalizedDataDirectory.resolve(VERSION_MARKER);
        Path temporaryMarker = Files.createTempFile(normalizedDataDirectory, VERSION_MARKER, ".tmp");
        try {
            Files.writeString(temporaryMarker, "version=" + currentVersion + System.lineSeparator()
                    + "sha256=" + sha256(pluginArtifact), StandardCharsets.UTF_8);
            move(temporaryMarker, marker);
        } catch (IOException exception) {
            Files.deleteIfExists(temporaryMarker);
            throw exception;
        }
    }

    private static PluginVersion readPluginVersion(Path marker) throws IOException {
        if (!Files.isRegularFile(marker)) {
            return null;
        }
        String contents = Files.readString(marker, StandardCharsets.UTF_8);
        String version = null;
        String sha256 = null;
        for (String line : contents.split("\\R")) {
            if (line.startsWith("version=")) {
                version = line.substring("version=".length());
            } else if (line.startsWith("sha256=")) {
                sha256 = line.substring("sha256=".length());
            }
        }
        if (version == null) {
            version = contents.trim();
        }
        return new PluginVersion(version, sha256);
    }

    private static String sha256(Path pluginArtifact) throws IOException {
        if (pluginArtifact == null) {
            return "unavailable";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(pluginArtifact)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static boolean containsData(Path dataDirectory, Path marker) throws IOException {
        if (!Files.isDirectory(dataDirectory)) {
            return false;
        }
        try (var paths = Files.walk(dataDirectory)) {
            return paths.anyMatch(path -> !path.equals(dataDirectory) && !path.equals(marker));
        }
    }

    private static void writeArchive(Path dataDirectory, Path archive) throws IOException {
        try (OutputStream fileOutput = Files.newOutputStream(archive);
             ZipOutputStream zipOutput = new ZipOutputStream(fileOutput)) {
            Files.walkFileTree(dataDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                        throws IOException {
                    if (!directory.equals(dataDirectory)) {
                        addDirectory(directory);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (!attributes.isRegularFile()) {
                        throw new IOException("Cannot safely include non-regular plugin data: " + file);
                    }
                    ZipEntry entry = new ZipEntry(entryName(dataDirectory.relativize(file)));
                    zipOutput.putNextEntry(entry);
                    try (InputStream input = Files.newInputStream(file)) {
                        input.transferTo(zipOutput);
                    }
                    zipOutput.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                private void addDirectory(Path directory) throws IOException {
                    ZipEntry entry = new ZipEntry(entryName(dataDirectory.relativize(directory)) + "/");
                    zipOutput.putNextEntry(entry);
                    zipOutput.closeEntry();
                }
            });
        }
    }

    private static void verifyArchive(Path archive) throws IOException {
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            if (zipFile.size() == 0) {
                throw new IOException("The plugin data backup archive is empty.");
            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    try (InputStream input = zipFile.getInputStream(entry)) {
                        input.transferTo(OutputStream.nullOutputStream());
                    } catch (IOException exception) {
                        throw exception;
                    }
                }
            }
        }
    }

    private static String entryName(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path uniqueArchivePath(Path requestedPath) {
        Path candidate = requestedPath;
        int suffix = 1;
        while (Files.exists(candidate)) {
            String filename = requestedPath.getFileName().toString();
            int extensionIndex = filename.lastIndexOf('.');
            String baseName = filename.substring(0, extensionIndex);
            candidate = requestedPath.resolveSibling(baseName + "-" + suffix++ + ".zip");
        }
        return candidate;
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record PluginVersion(String version, String sha256) {
    }
}
