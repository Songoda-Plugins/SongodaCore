package com.songoda.core.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class HttpResponseImpl implements HttpResponse, AutoCloseable {
    protected final HttpURLConnection connection;

    protected byte[] body;

    HttpResponseImpl(HttpURLConnection connection) throws IOException {
        this.connection = connection;

        this.connection.connect();
    }

    public int getResponseCode() throws IOException {
        int statusCode = this.connection.getResponseCode();

        if (statusCode == -1) {
            throw new IOException("HTTP Status Code is -1");
        }

        return statusCode;
    }

    public byte[] getBody() throws IOException {
        if (this.body == null) {
            try (InputStream err = this.connection.getErrorStream()) {
                if (err != null) {
                    this.body = toByteArray(err);
                    return this.body;
                }
            }

            try (InputStream in = this.connection.getInputStream()) {
                this.body = toByteArray(in);
            }
        }

        return this.body;
    }

    public String getBodyAsString() throws IOException {
        return new String(getBody(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws Exception {
        this.connection.disconnect();
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = in.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }
}
