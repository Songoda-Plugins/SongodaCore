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

    private static ReflectionMethod reflectionMethod = null;
    private static Method skullMetaSetProfileMethod = null;
    private static Field skullMetaProfileField = null;

    private enum ReflectionMethod {
        RESOLVABLE_PROFILE,  // 1.21.9+ using ResolvableProfile
        GAME_PROFILE,        // 1.18-1.21.8 using GameProfile directly
        FIELD_ACCESS         // Pre-1.18 or fallback using direct field access
    }

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
                    })
                    .exceptionally(ex -> createDefaultSkullForUuid(uuid));
        } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
        }

        if (uuid.version() != 4) {
            return CompletableFuture.completedFuture(createDefaultSkullForUuid(uuid));
        }

        return minecraftApiClient.fetchProfile(uuid)
                .thenApply(profile -> {
                    if (profile == null) {
                        return createDefaultSkullForUuid(uuid);
                    }
                    return byProfile(profile.createGameProfile());
                })
                .exceptionally(ex -> createDefaultSkullForUuid(uuid));
    }

    private static void applyProfile(SkullMeta skullMeta, GameProfile profile) {
        if (reflectionMethod == null) {
            initializeReflection(skullMeta, profile);
        }

        try {
            switch (reflectionMethod) {
                case RESOLVABLE_PROFILE: {
                    // 1.21.9+ - Use ResolvableProfile
                    Object resolvableProfile = profile.getMojangResolvableGameProfile();
                    if (resolvableProfile == null) {
                        throw new IllegalStateException("ResolvableProfile is null for 1.21.9+ version");
                    }
                    skullMetaSetProfileMethod.invoke(skullMeta, resolvableProfile);
                    break;
                }
                case GAME_PROFILE: {
                    // 1.18-1.21.8 - Use GameProfile directly
                    Object gameProfile = profile.getMojangGameProfile();
                    if (gameProfile == null) {
                        throw new IllegalStateException("GameProfile is null");
                    }
                    skullMetaSetProfileMethod.invoke(skullMeta, gameProfile);
                    break;
                }
                    // Pre 1.18
                case FIELD_ACCESS: {
                    Object toSet;
                    Object resolvable = profile.getMojangResolvableGameProfile();
                    Object game = profile.getMojangGameProfile();
                    Class<?> fieldType = skullMetaProfileField.getType();
                    if (resolvable != null && fieldType.isInstance(resolvable)) {
                        toSet = resolvable;
                    } else if (game != null && fieldType.isInstance(game)) {
                        toSet = game;
                    } else if (resolvable != null && fieldType.getName().contains("ResolvableProfile")) {
                        toSet = resolvable;
                    } else {
                        toSet = game;
                    }
                    if (toSet == null) {
                        throw new IllegalStateException("No compatible profile instance for field access: " + fieldType.getName());
                    }
                    skullMetaProfileField.set(skullMeta, toSet);
                    break;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to apply GameProfile to SkullMeta using " + reflectionMethod, ex);
        }
    }

    private static void initializeReflection(SkullMeta skullMeta, GameProfile profile) {
        Class<?> skullMetaClass = skullMeta.getClass();

        Object resolvableProfile = profile.getMojangResolvableGameProfile();
        if (resolvableProfile != null) {
            try {
                // Prefer public method lookup (includes inherited)
                skullMetaSetProfileMethod = skullMetaClass.getMethod("setProfile", resolvableProfile.getClass());
                reflectionMethod = ReflectionMethod.RESOLVABLE_PROFILE;
                return;
            } catch (NoSuchMethodException ignored) {
                for (Method m : skullMetaClass.getMethods()) {
                    if (!m.getName().equals("setProfile") || m.getParameterCount() != 1) continue;
                    Class<?> p0 = m.getParameterTypes()[0];
                    if (p0.getName().contains("ResolvableProfile")) {
                        m.setAccessible(true);
                        skullMetaSetProfileMethod = m;
                        reflectionMethod = ReflectionMethod.RESOLVABLE_PROFILE;
                        return;
                    }
                }
                for (Method m : skullMetaClass.getDeclaredMethods()) {
                    if (!m.getName().equals("setProfile") || m.getParameterCount() != 1) continue;
                    Class<?> p0 = m.getParameterTypes()[0];
                    if (p0.getName().contains("ResolvableProfile")) {
                        m.setAccessible(true);
                        skullMetaSetProfileMethod = m;
                        reflectionMethod = ReflectionMethod.RESOLVABLE_PROFILE;
                        return;
                    }
                }
            }
        }

        Object gameProfile = profile.getMojangGameProfile();
        if (gameProfile != null) {
            try {
                skullMetaSetProfileMethod = skullMetaClass.getMethod("setProfile", gameProfile.getClass());
                reflectionMethod = ReflectionMethod.GAME_PROFILE;
                return;
            } catch (NoSuchMethodException ignored) {
                for (Method m : skullMetaClass.getMethods()) {
                    if (!m.getName().equals("setProfile") || m.getParameterCount() != 1) continue;
                    Class<?> p0 = m.getParameterTypes()[0];
                    if (p0.getName().contains("GameProfile")) {
                        m.setAccessible(true);
                        skullMetaSetProfileMethod = m;
                        reflectionMethod = ReflectionMethod.GAME_PROFILE;
                        return;
                    }
                }
                for (Method m : skullMetaClass.getDeclaredMethods()) {
                    if (!m.getName().equals("setProfile") || m.getParameterCount() != 1) continue;
                    Class<?> p0 = m.getParameterTypes()[0];
                    if (p0.getName().contains("GameProfile")) {
                        m.setAccessible(true);
                        skullMetaSetProfileMethod = m;
                        reflectionMethod = ReflectionMethod.GAME_PROFILE;
                        return;
                    }
                }
            }
        }

        try {
            skullMetaProfileField = skullMetaClass.getDeclaredField("profile");
            skullMetaProfileField.setAccessible(true);
            reflectionMethod = ReflectionMethod.FIELD_ACCESS;
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(
                    "Unable to find compatible method to apply GameProfile to SkullMeta.",
                    ex
            );
        }
    }

    public static void resetReflectionCache() {
        reflectionMethod = null;
        skullMetaSetProfileMethod = null;
        skullMetaProfileField = null;
    }
}
