package com.songoda.core.database;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class H2DatabaseConversionClientTest {

    @Test
    public void uploadsRawDatabaseWithCredentialsAndSavesBinaryResponse() throws Exception {
        byte[] input = new byte[]{1, 2, 3, 4};
        byte[] converted = new byte[]{5, 6, 7, 8, 9};
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<String> password = new AtomicReference<>();
        AtomicReference<byte[]> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/database-convert/v1/h2", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            username.set(exchange.getRequestHeaders().getFirst("X-H2-Username"));
            password.set(exchange.getRequestHeaders().getFirst("X-H2-Password"));
            requestBody.set(exchange.getRequestBody().readAllBytes());
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, converted.length);
            exchange.getResponseBody().write(converted);
            exchange.close();
        });
        server.start();

        Path folder = Files.createTempDirectory("songoda-h2-conversion-client");
        Path source = folder.resolve("legacy.mv.db");
        Path destination = folder.resolve("converted.mv.db");
        Files.write(source, input);
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/api/database-convert/v1/h2");
            new H2DatabaseConversionClient(endpoint).convert(source, destination, "custom-user", "secret");

            assertEquals("application/octet-stream", contentType.get());
            assertEquals("custom-user", username.get());
            assertEquals("secret", password.get());
            assertArrayEquals(input, requestBody.get());
            assertArrayEquals(converted, Files.readAllBytes(destination));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void rejectsInputLargerThanServiceLimitBeforeUploading() throws Exception {
        Path folder = Files.createTempDirectory("songoda-h2-conversion-size-limit");
        Path source = folder.resolve("large.mv.db");
        try (var file = new java.io.RandomAccessFile(source.toFile(), "rw")) {
            file.setLength(H2DatabaseConversionClient.MAX_DATABASE_SIZE + 1);
        }
        try {
            new H2DatabaseConversionClient(URI.create("http://127.0.0.1:1/convert"))
                    .convert(source, folder.resolve("converted.mv.db"), "sa", "");
            fail("Expected oversized input to be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("100 MB"));
        }
    }

    @Test
    public void reportsServiceRejectionAndRemovesErrorBodyFile() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/convert", exchange -> {
            byte[] body = "This is not a supported H2 1.4 database".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        Path folder = Files.createTempDirectory("songoda-h2-conversion-rejection");
        Path source = folder.resolve("legacy.mv.db");
        Path destination = folder.resolve("converted.mv.db");
        Files.write(source, new byte[]{1, 2, 3});
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/convert");
            try {
                new H2DatabaseConversionClient(endpoint).convert(source, destination, "sa", "");
                fail("Expected the converter error response to fail");
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains("HTTP 422"));
                assertTrue(expected.getMessage().contains("not a supported H2"));
                assertTrue(Files.notExists(destination));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void reportsUnavailableService() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.start();
        server.stop(0);
        Path folder = Files.createTempDirectory("songoda-h2-conversion-unavailable");
        Path source = folder.resolve("legacy.mv.db");
        Files.write(source, new byte[]{1, 2, 3});

        try {
            new H2DatabaseConversionClient(URI.create("http://127.0.0.1:" + port + "/convert"))
                    .convert(source, folder.resolve("converted.mv.db"), "sa", "");
            fail("Expected unavailable service to fail");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("Could not contact Songoda's H2 conversion service"));
        }
    }
}
