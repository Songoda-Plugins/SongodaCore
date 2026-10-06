package com.songoda.core.compatibility;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves enchantments through the current Paper registry API while remaining compatible with older servers.
 */
public final class EnchantmentResolver {

    private static final Map<String, Enchantment> RESOLUTION_CACHE = new ConcurrentHashMap<>();

    private EnchantmentResolver() {
    }

    public static @Nullable Enchantment resolve(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        Enchantment cached = RESOLUTION_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }

        NamespacedKey key;
        if (normalized.contains(":")) {
            key = NamespacedKey.fromString(normalized);
        } else {
            try {
                key = NamespacedKey.minecraft(normalized);
            } catch (IllegalArgumentException ignored) {
                key = null;
            }
        }

        Enchantment enchantment = key != null ? resolve(key) : null;
        if (enchantment == null) {
            try {
                enchantment = Enchantment.getByName(normalized.toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {
            }
        }

        if (enchantment == null) {
            enchantment = resolveStaticField(normalized);
        }

        if (enchantment != null) {
            RESOLUTION_CACHE.put(normalized, enchantment);
        }

        return enchantment;
    }

    public static @Nullable Enchantment resolve(@Nullable NamespacedKey key) {
        if (key == null) {
            return null;
        }

        String cacheKey = key.toString();
        Enchantment cached = RESOLUTION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Enchantment enchantment = null;

        try {
            if (ServerVersion.isAtLeastVersion(KnownServerVersions.V1_20_6)) {
                enchantment = resolveWithRegistryAccess(key);
            }
        } catch (Throwable ignored) {
        }

        if (enchantment == null) {
            enchantment = resolveWithLegacyRegistry(key);
        }

        if (enchantment == null) {
            try {
                enchantment = Enchantment.getByKey(key);
            } catch (Throwable ignored) {
            }
        }

        if (enchantment == null) {
            try {
                enchantment = Enchantment.getByName(key.getKey().toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {
            }
        }

        if (enchantment == null) {
            enchantment = resolveStaticField(key.getKey());
        }

        if (enchantment != null) {
            RESOLUTION_CACHE.put(cacheKey, enchantment);
            RESOLUTION_CACHE.put(key.getKey().toLowerCase(Locale.ROOT), enchantment);
        }

        return enchantment;
    }

    private static @Nullable Enchantment resolveWithRegistryAccess(NamespacedKey key) {
        try {
            Class<?> registryAccessType = Class.forName("io.papermc.paper.registry.RegistryAccess");
            Class<?> registryKeyType = Class.forName("io.papermc.paper.registry.RegistryKey");
            Object registryAccess = registryAccessType.getMethod("registryAccess").invoke(null);
            Object enchantmentKey = registryKeyType.getField("ENCHANTMENT").get(null);
            Object registry = registryAccessType.getMethod("getRegistry", registryKeyType).invoke(registryAccess, enchantmentKey);
            Method getMethod = registry.getClass().getMethod("get", NamespacedKey.class);
            Object enchantment = getMethod.invoke(registry, key);
            return enchantment instanceof Enchantment resolved ? resolved : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static @Nullable Enchantment resolveWithLegacyRegistry(NamespacedKey key) {
        try {
            Field enchantmentRegistryField = Registry.class.getField("ENCHANTMENT");
            Object enchantmentRegistry = enchantmentRegistryField.get(null);
            Method getMethod = Registry.class.getMethod("get", NamespacedKey.class);
            Object enchantment = getMethod.invoke(enchantmentRegistry, key);
            return enchantment instanceof Enchantment resolved ? resolved : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static @Nullable Enchantment resolveStaticField(String name) {
        try {
            String fieldName = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
            Field field = Enchantment.class.getField(fieldName.toUpperCase(Locale.ROOT));
            Object val = field.get(null);
            return val instanceof Enchantment resolved ? resolved : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
