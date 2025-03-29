package com.songoda.core.nms.v1_16_R2.entity;

import com.songoda.core.nms.entity.NMSPlayer;
import com.songoda.core.nms.entity.player.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_16_R2.Packet;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NMSPlayerImpl implements NMSPlayer {
    @Override
    public void sendPacket(Player p, Object packet) {
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet<?>) packet);
    }

    @Override
    public GameProfile getProfile(Player p) {
        com.mojang.authlib.GameProfile profile = ((CraftPlayer) p).getHandle().getProfile();
        return wrapProfile(profile);
    }

    @Override
    public GameProfile createProfile(UUID id, String name, @Nullable String textureValue, @Nullable String textureSignature) {
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(id, name);
        if (textureValue != null) {
            profile.getProperties().put("textures", new Property("textures", textureValue, textureSignature));
        }

        return wrapProfile(profile);
    }

    private GameProfile wrapProfile(com.mojang.authlib.GameProfile profile) {
        String textureValue = null;
        String textureSignature = null;
        for (Property property : profile.getProperties().get("textures")) {
            if (property.getName().equals("textures")) {
                textureValue = property.getValue();
                textureSignature = property.getSignature();
            }
        }

        return new GameProfile(
                profile,
                null,

                profile.getId(),
                profile.getName(),
                textureValue,
                textureSignature
        );
    }
}
