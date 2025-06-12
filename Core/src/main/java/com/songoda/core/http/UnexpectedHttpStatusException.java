package com.songoda.core.http;

import java.io.IOException;

public class UnexpectedHttpStatusException extends IOException {
    public final int responseCode;
    public final String url;

    public UnexpectedHttpStatusException(int responseCode, String url) {
        super("Got HTTP Status Code " + responseCode + ": " + url);

        this.responseCode = responseCode;
        this.url = url;
    }
}
