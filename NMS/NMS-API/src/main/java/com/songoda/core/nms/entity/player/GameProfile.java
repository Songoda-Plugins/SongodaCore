package com.songoda.core.nms.entity.player;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GameProfile {
    private final Object mojangGameProfile;
    private final Object mojangResolvableGameProfile;

    private final UUID id;
    private final String name;
    private final String textureValue;
    private final String textureSignature;

    public GameProfile(
            Object mojangGameProfile,
            @Nullable Object mojangResolvableGameProfile,
            UUID id,
            String name,
            @Nullable String textureValue,
            @Nullable String textureSignature
    ) {
        this.mojangGameProfile = mojangGameProfile;
        this.mojangResolvableGameProfile = mojangResolvableGameProfile;
        this.id = id;
        this.name = name;
        this.textureValue = textureValue;
        this.textureSignature = textureSignature;
    }

    public Object getMojangGameProfile() {
        return mojangGameProfile;
    }

    @Nullable
    public Object getMojangResolvableGameProfile() {
        return mojangResolvableGameProfile;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getTextureValue() {
        return textureValue;
    }

    @Nullable
    public String getTextureSignature() {
        return textureSignature;
    }

    public boolean hasTexture() {
        return textureValue != null;
    }
}
