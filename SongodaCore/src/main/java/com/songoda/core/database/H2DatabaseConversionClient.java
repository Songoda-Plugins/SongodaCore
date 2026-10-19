package com.songoda.core.database;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Client for Songoda's hosted H2 1.4 to H2 2.x database converter.
 */
public final class H2DatabaseConversionClient {

    public static final URI DEFAULT_ENDPOINT = URI.create("https://songoda-reborn.com/api/database-convert/v1/h2");
    public static final long MAX_DATABASE_SIZE = 100L * 1024L * 1024L;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final URI endpoint;

    /**
     * Creates a client that calls the production Songoda converter.
     */
    public H2DatabaseConversionClient() {
        this(DEFAULT_ENDPOINT);
    }

    /**
     * Creates a client for the given endpoint. This constructor is useful for local deployments and tests.
     *
     * @param endpoint converter endpoint
     */
    public H2DatabaseConversionClient(URI endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Converts a closed H2 1.4 database file and writes the returned H2 2.x file to {@code convertedDatabaseFile}.
     *
     * @param legacyDatabaseFile closed legacy {@code .mv.db} file
     * @param convertedDatabaseFile destination for the converted {@code .mv.db} file
     * @param username H2 database username
     * @param password H2 database password
     * @throws IOException if the file is invalid, the request fails, or the converter rejects the database
     */
    public void convert(Path legacyDatabaseFile, Path convertedDatabaseFile, String username, String password)
            throws IOException {
        if (!Files.isRegularFile(legacyDatabaseFile)) {
            throw new IOException("The legacy H2 database file does not exist: " + legacyDatabaseFile);
        }
        long inputSize = Files.size(legacyDatabaseFile);
        if (inputSize == 0 || inputSize > MAX_DATABASE_SIZE) {
            throw new IOException("The legacy H2 database must be between 1 byte and 100 MB.");
        }
        String safeUsername = username == null ? "sa" : username;
        String safePassword = password == null ? "" : password;
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/octet-stream")
                    .header("X-H2-Username", safeUsername)
                    .header("X-H2-Password", safePassword)
                    .POST(HttpRequest.BodyPublishers.ofFile(legacyDatabaseFile))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new IOException("The hosted H2 conversion endpoint is invalid.", exception);
        }

        Path destination = convertedDatabaseFile.toAbsolutePath().normalize();
        Files.createDirectories(destination.getParent());
        Files.deleteIfExists(destination);
        try {
            HttpResponse<Path> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofFile(destination));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String details = readError(destination);
                String message = "H2 conversion service returned HTTP " + response.statusCode()
                        + (details.isBlank() ? "." : ": " + details);
                throw new IOException(message);
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/octet-stream")) {
                throw new IOException("H2 conversion service returned an unexpected response type.");
            }
            if (Files.size(destination) == 0) {
                throw new IOException("H2 conversion service returned an empty database file.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(destination);
            throw new IOException("H2 database conversion was interrupted.", exception);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
    }

    private static String readError(Path responseFile) throws IOException {
        try (var input = Files.newInputStream(responseFile)) {
            byte[] bytes = input.readNBytes(4096);
            return new String(bytes, StandardCharsets.UTF_8).replaceAll("[\\r\\n\\t]+", " ").trim();
        }
    }
}
