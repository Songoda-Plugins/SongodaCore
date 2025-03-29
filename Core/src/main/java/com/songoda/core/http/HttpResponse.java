package com.songoda.core.http;

import java.io.IOException;

public interface HttpResponse {
    int getResponseCode() throws IOException;

    byte[] getBody() throws IOException;

    String getBodyAsString() throws IOException;
}
