package com.songoda.core.http.minecraft;

import com.songoda.core.nms.Nms;
import com.songoda.core.nms.entity.player.GameProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
public class UuidToProfileResult {
    private static final Pattern ADD_HYPHENS_TO_UUID = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{3})(\\w{12})");

    private @Nullable String id;
    private @Nullable String name;
    private @NotNull List<Property> properties = Collections.emptyList();
    private @NotNull List<String> profileActions = Collections.emptyList();

    public @NotNull UUID getId() {
        return UUID.fromString(
                ADD_HYPHENS_TO_UUID
                        .matcher(Objects.requireNonNull(this.id))
                        .replaceAll("$1-$2-$3-$4-$5")
        );
    }

    public @NotNull String getName() {
        return Objects.requireNonNull(this.name);
    }

    public @NotNull List<Property> getProperties() {
        return this.properties;
    }

    public @NotNull List<String> getProfileActions() {
        return this.profileActions;
    }

    public @NotNull GameProfile createGameProfile() {
        String textureValue = null;
        String textureSignature = null;
        for (Property property : this.properties) {
            if (property.name.equals("textures")) {
                textureValue = property.value;
                textureSignature = property.signature;
            }
        }

        return Nms.getImplementations().getPlayer().createProfile(getId(), getName(), textureValue, textureSignature);
    }

    public static class Property {
        private String name;
        private String value;
        private @Nullable String signature;
    }
}
