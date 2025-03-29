package com.songoda.core.http;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public interface HttpClient {
    default @NotNull HttpResponse request(String method, String url) throws IOException {
        return request(method, url, null, null);
    }

    default @NotNull HttpResponse request(String method, String url, Map<String, String> headers) throws IOException {
        return request(method, url, headers, null);
    }

    @NotNull HttpResponse request(String method, String url, Map<String, String> headers, byte[] body) throws IOException;

    default @NotNull HttpResponse get(String url) throws IOException {
        return request("GET", url);
    }

    default @NotNull HttpResponse post(String url, Map<String, String> headers, byte[] body) throws IOException {
        return request("POST", url, headers, body);
    }
}
