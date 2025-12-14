package com.songoda.core.nms.v1_21_R7.entity;

import com.mojang.authlib.properties.Property;
import com.songoda.core.nms.entity.NMSPlayer;
import com.songoda.core.nms.entity.player.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.component.ResolvableProfile;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NMSPlayerImpl implements NMSPlayer {
    @Override
    public void sendPacket(Player p, Object packet) {
        ((CraftPlayer) p).getHandle().connection.send((Packet<?>) packet);
    }

    @Override
    public GameProfile getProfile(Player p) {
        com.mojang.authlib.GameProfile mojangProfile = ((CraftPlayer) p).getHandle().getGameProfile();
        return wrapProfile(mojangProfile);
    }

    @Override
    public GameProfile createProfile(UUID id, String name, @Nullable String textureValue, @Nullable String textureSignature) {
        if (textureValue == null) {
            return wrapProfile(new com.mojang.authlib.GameProfile(id, name));
        }

        com.google.common.collect.Multimap<String, Property> properties = com.google.common.collect.ArrayListMultimap.create();
        properties.put("textures", new Property("textures", textureValue,
            textureSignature != null ? textureSignature : ""));


        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
            id,
            name,
            new com.mojang.authlib.properties.PropertyMap(properties)
        );

        return wrapProfile(profile);
    }

    private GameProfile wrapProfile(com.mojang.authlib.GameProfile mojangProfile) {
        String textureValue = null;
        String textureSignature = null;
        for (Property property : mojangProfile.properties().get("textures")) {
            if (property.name().equals("textures")) {
                textureValue = property.value();
                textureSignature = property.signature();
            }
        }

        ResolvableProfile resolvableProfile = ResolvableProfile.createResolved(mojangProfile);

        return new GameProfile(
                mojangProfile,
                resolvableProfile,
                mojangProfile.id(),
                mojangProfile.name(),
                textureValue,
                textureSignature
        );
    }
}
