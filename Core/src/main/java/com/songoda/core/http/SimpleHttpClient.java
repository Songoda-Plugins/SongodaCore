package com.songoda.core.http;

import com.songoda.core.SongodaCoreConstants;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class SimpleHttpClient implements HttpClient {
    private static final String USER_AGENT = generateUserAgent();

    @Override
    public @NotNull HttpResponse request(String method, String url, Map<String, String> headers, byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        if (body != null && body.length > 0) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(body);
        }

        return new HttpResponseImpl(connection);
    }

    private static String generateUserAgent() {
        String projectName = SongodaCoreConstants.getProjectName();
        String version = SongodaCoreConstants.getCoreVersion();
        String projectUrl = SongodaCoreConstants.getGitHubProjectUrl();

        return projectName + "/" + version + " (+" + projectUrl + ")";
    }
}
