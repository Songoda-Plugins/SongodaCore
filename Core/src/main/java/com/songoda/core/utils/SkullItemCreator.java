package com.songoda.core.utils;

import com.songoda.core.http.SimpleHttpClient;
import com.songoda.core.http.minecraft.MinecraftApiClient;
import com.songoda.core.nms.Nms;
import com.songoda.core.nms.entity.player.GameProfile;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkullItemCreator {
    private static final String STEVE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYyMTcxNTMxMjI5MCwKICAicHJvZmlsZUlkIiA6ICJiNTM5NTkyMjMwY2I0MmE0OWY5YTRlYmYxNmRlOTYwYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJpYW5hZmFnIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFhNGFmNzE4NDU1ZDRhYWI1MjhlN2E2MWY4NmZhMjVlNmEzNjlkMTc2OGRjYjEzZjdkZjMxOWE3MTNlYjgxMGIiCiAgICB9CiAgfQp9";
    private static final String ALEX_TEXTURE = "rZvLQoZsgLYaoKqEuASopYAs7IAlZlsGkwagoM8ZX38cP9kalseZrWY5OHZVfoiftdQJ+lGOzkiFfyx6kNJDTZniLrnRa8sd3X6D65ZihT1sOm/RInCwxpS1K0zGCM2h9ErkWswfwaviIf7hJtrwk8/zL0bfzDk2IgX/IBvIZpVoYTfmQsVY9jgSwORrS9ObePGIfFgmThMoZnCYWQMVpS2+yTFA2wnw9hmisQK9UWBU+iBZv55bMmkMcyEuXw1w14DaEu+/M0UGD91LU4GmJLPA9T4GCuIV8GxOcraSVIajki1cMlOBQwIaibB2NE6KAwq1Zh6NnsNYucy6qFM+136lXfBchQ1Nx4FDRZQgt8VRqTMy/OQFpr2nTbWWbRU4gRFpKC3R0518DqUH0Qm612kPWniKku/QzUUBSe1PSVljBaZCyyRx0OB1a1/8MexboKRnPXuTDnmPa9UPfuH4VO0q+qYkjV2KUzP6e5vIP5aQ6USPrMie7MmAHFJzwAMIbLjgkTVx91GWtYqg/t7qBlvrdBRLIPPsy/DSOqa+2+4hABouVCPZrBMCMLzstPPQoqZAyiCqcKb2HqWSU0h9Bhx19yoIcbHCeI3zsQs8PqIBjUL4mO6VQT4lzHy0e3M61Xsdd8S1GtsakSetTvEtMdUwCEDfBA5PRRTLOVYTY+g=";

    private static MinecraftApiClient minecraftApiClient = new MinecraftApiClient(new SimpleHttpClient());

    public static ItemStack byProfile(GameProfile profile) {
        ItemStack item = Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem());
        SkullMeta meta = (SkullMeta) Objects.requireNonNull(item.getItemMeta());
        applyProfile(meta, profile);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack byPlayer(Player player) {
        return byProfile(Nms.getImplementations().getPlayer().getProfile(player));
    }

    public static ItemStack byTextureValue(String textureValue) {
        return byProfile(Nms.getImplementations().getPlayer().createProfileByTextureValue(textureValue));
    }

    public static ItemStack byTextureUrl(String textureUrl) {
        return byProfile(Nms.getImplementations().getPlayer().createProfileByUrl(textureUrl));
    }

    public static ItemStack byTextureUrlHash(String textureUrlHash) {
        return byTextureUrl("https://textures.minecraft.net/texture/" + textureUrlHash);
    }

    public static ItemStack createSteve() {
        return byTextureValue(STEVE_TEXTURE);
    }

    public static ItemStack createAlex() {
        return byTextureValue(ALEX_TEXTURE);
    }

    public static ItemStack createDefaultSkullForUuid(UUID uuid) {
        if ((uuid.hashCode() & 1) != 0) {
            return byTextureValue(ALEX_TEXTURE);
        }
        return byTextureValue(STEVE_TEXTURE);
    }

    /**
     * This method tries its best in determining the correct skin for a given UUID.
     * It tries to request the corresponding profile (with skin data) from Mojang's API.
     */
    public static CompletableFuture<ItemStack> byUuid(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return CompletableFuture.completedFuture(byPlayer(player));
        }

        try {
            return Bukkit.getOfflinePlayer(uuid)
                    .getPlayerProfile()
                    .update()
                    .thenApply(profile -> {
                        if (profile.getTextures().getSkin() == null) {
                            return createDefaultSkullForUuid(uuid);
                        }
                        return byTextureUrl(profile.getTextures().getSkin().toString());
                    });
        } catch (NoSuchMethodError ignored) {
            // old spigot api version that doesn't have OfflinePlayer#getPlayerProfile()
        }

        if (uuid.version() != 4) {
            return CompletableFuture.completedFuture(createDefaultSkullForUuid(uuid));
        }

        return minecraftApiClient.fetchProfile(uuid)
                .thenApply((profile) -> {
                    if (profile == null) {
                        return createDefaultSkullForUuid(uuid);
                    }
                    return byProfile(profile.createGameProfile());
                });
    }

    private static Method skullMetaSetProfile = null;
    private static Field skullMetaProfileField = null;
    private static boolean setProfileUsesResolvable = false;

    private static void applyProfile(SkullMeta skullMeta, GameProfile profile) {
        if (skullMetaSetProfile == null && profile.getMojangResolvableGameProfile() != null) {
            try {
                skullMetaSetProfile = skullMeta.getClass().getDeclaredMethod("setProfile", profile.getMojangResolvableGameProfile().getClass());
                skullMetaSetProfile.setAccessible(true);
                setProfileUsesResolvable = true;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (skullMetaSetProfile == null) {
            try {
                skullMetaSetProfile = skullMeta.getClass().getDeclaredMethod("setProfile", profile.getMojangGameProfile().getClass());
                skullMetaSetProfile.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        if (skullMetaSetProfile != null) {
            try {
                skullMetaSetProfile.invoke(skullMeta, setProfileUsesResolvable ? profile.getMojangResolvableGameProfile() : profile.getMojangGameProfile());
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            return;
        }

        if (skullMetaProfileField == null) {
            try {
                skullMetaProfileField = skullMeta.getClass().getDeclaredField("profile");
                skullMetaProfileField.setAccessible(true);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Unable to find compatible #setProfile method or profile field", ex);
            }
        }

        try {
            skullMetaProfileField.set(skullMeta, profile.getMojangGameProfile());
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Encountered an error while setting the profile field", ex);
        }
    }
}
