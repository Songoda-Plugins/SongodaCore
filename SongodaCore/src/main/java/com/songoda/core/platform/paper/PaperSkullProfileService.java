package com.songoda.core.platform.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.spi.SkullProfileService;
import com.songoda.core.spi.SkullProfiles;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@net.vortexdevelopment.vinject.annotation.component.Component
public class PaperSkullProfileService implements SkullProfileService {

    @PostConstruct
    public void registerSkullProfile() {
        if (!ServerProject.isPaperCompatible()) {
            return;
        }
        SkullProfiles.install(this);
    }

    @Override
    public void applyTexture(ItemStack head, String textureUrl) {
        applyTexture(head, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void applyTexture(ItemStack head, String textureUrl, UUID profileId) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("ItemStack does not have SkullMeta");
        }
        applyTexture(meta, textureUrl, profileId);
        head.setItemMeta(meta);
    }

    @Override
    public void applyTexture(ItemMeta meta, String textureUrl) {
        applyTexture(meta, textureUrl, UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void applyTexture(ItemMeta meta, String textureUrl, UUID profileId) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new IllegalArgumentException("ItemMeta is not SkullMeta");
        }
        try {
            PlayerProfile profile = Bukkit.createProfile(profileId);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            skullMeta.setPlayerProfile(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serializeSkull(SkullMeta skullMeta, Map<String, Object> map) {
        PlayerProfile profile = skullMeta.getPlayerProfile();
        if (profile != null) {
            if (profile.getId() != null) {
                map.put("UUID", profile.getId().toString());
            }
            PlayerTextures textures = profile.getTextures();
            if (textures != null && textures.getSkin() != null) {
                map.put("Texture", textures.getSkin().toString());
            } else if (profile.getName() != null) {
                map.put("Owner", profile.getName());
            }
        }
    }

    @Override
    public void deserializeSkull(SkullMeta skullMeta, Map<String, Object> map) {
        UUID uuid = null;
        if (map.containsKey("UUID")) {
            try {
                uuid = UUID.fromString((String) map.get("UUID"));
            } catch (Exception ignored) {
            }
        }

        if (map.containsKey("Texture")) {
            String texture = (String) map.get("Texture");
            if (uuid != null) {
                applyTexture(skullMeta, texture, uuid);
            } else {
                applyTexture(skullMeta, texture);
            }
        } else if (map.containsKey("Owner")) {
            String owner = (String) map.get("Owner");
            PlayerProfile profile = uuid != null ? Bukkit.createProfile(uuid, owner) : Bukkit.createProfile(owner);
            skullMeta.setPlayerProfile(profile);
        }
    }
}
