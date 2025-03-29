package com.songoda.core.http.minecraft;

import com.songoda.core.SongodaCore;
import com.songoda.core.http.HttpClient;
import com.songoda.core.http.HttpResponse;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MinecraftApiClient {
    private static final String SPRAX_API_URL = "https://api.sprax2013.de/mc/profile/";
    private static final String MOJANG_API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    private long nextSpraxApiRequestAllowed = 0;

    public MinecraftApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletableFuture<@Nullable UuidToProfileResult> fetchProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            HttpResponse profileResponse;
            try {
                if (this.nextSpraxApiRequestAllowed <= System.currentTimeMillis()) {
                    throw new IOException("Not allowed to use SpraxAPI");
                }

                this.nextSpraxApiRequestAllowed = 0;
                profileResponse = this.httpClient.get(SPRAX_API_URL + uuid);
            } catch (IOException ignored) {
                try {
                    profileResponse = this.httpClient.get(MOJANG_API_URL + uuid.toString().replace("-", ""));

                    if (this.nextSpraxApiRequestAllowed <= System.currentTimeMillis()) {
                        this.nextSpraxApiRequestAllowed = System.currentTimeMillis() + 1000 * 60 * 5;
                        SongodaCore.getLogger().warning("Error contacting api.sprax2013.de while sessionserver.mojang.com is working. Not using SpraxAPI for the next 5 minutes...");
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            try {
                if (profileResponse.getResponseCode() == 404 || profileResponse.getResponseCode() == 204) {
                    return null;
                }

                return this.gson.fromJson(profileResponse.getBodyAsString(), UuidToProfileResult.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
