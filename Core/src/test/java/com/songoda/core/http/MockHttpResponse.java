package com.songoda.core.http;

import java.nio.charset.StandardCharsets;

public class MockHttpResponse implements HttpResponse {
    public int responseCode;
    public byte[] body;

    public MockHttpResponse(int responseCode, byte[] body) {
        this.responseCode = responseCode;
        this.body = body;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public byte[] getBody() {
        return this.body;
    }

    public String getBodyAsString() {
        return new String(getBody(), StandardCharsets.UTF_8);
    }
}
