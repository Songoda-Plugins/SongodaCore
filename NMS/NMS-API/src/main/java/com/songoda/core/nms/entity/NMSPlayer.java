package com.songoda.core.nms.entity;

import com.songoda.core.nms.entity.player.GameProfile;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public interface NMSPlayer {
    void sendPacket(Player p, Object packet);

    GameProfile getProfile(Player p);

    GameProfile createProfile(UUID id, String name, @Nullable String textureValue, @Nullable String textureSignature);

    default GameProfile createProfileByUrl(String url) {
        UUID id = UUID.nameUUIDFromBytes(("SongodaCore:" + url).getBytes(StandardCharsets.UTF_8));
        String rawTextureValue = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return createProfile(id, "by_SongodaCore", Base64.getEncoder().encodeToString(rawTextureValue.getBytes()), null);
    }

    default GameProfile createProfileByTextureValue(String textureValue) {
        UUID id = UUID.nameUUIDFromBytes(("SongodaCore:" + textureValue).getBytes(StandardCharsets.UTF_8));
        return createProfile(id, "by_SongodaCore", textureValue, null);
    }
}
