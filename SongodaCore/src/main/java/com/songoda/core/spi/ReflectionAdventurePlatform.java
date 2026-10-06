package com.songoda.core.spi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Uses Gson round-trip between the server's Adventure serializer and the plugin classpath {@link Component}.
 * Initialized whenever Bukkit exposes ItemMeta methods referencing Adventure.
 */
public enum ReflectionAdventurePlatform implements AdventurePlatform {
    INSTANCE;

    private final Class<?> componentClass;
    private final Object gsonComponentSerializer;
    private final Method gsonDeserializeMethod;
    private final Method gsonSerializeMethod;

    ReflectionAdventurePlatform() {
        Class<?> compClass = null;
        Object gsonSer = null;
        Method deserialize = null;
        Method serialize = null;
        try {
            compClass = Class.forName("net.kyori.adventure.text.Component");
            ItemMeta.class.getDeclaredMethod("displayName", compClass);
            ItemMeta.class.getDeclaredMethod("lore", List.class);
            gsonSer = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer")
                    .getDeclaredMethod("gson")
                    .invoke(null);
            deserialize = gsonSer.getClass().getDeclaredMethod("deserialize", String.class);
            deserialize.setAccessible(true);
            serialize = gsonSer.getClass().getDeclaredMethod("serialize", compClass);
            serialize.setAccessible(true);
        } catch (Exception ignored) {
        }
        this.componentClass = compClass;
        this.gsonComponentSerializer = gsonSer;
        this.gsonDeserializeMethod = deserialize;
        this.gsonSerializeMethod = serialize;
    }

    @Override
    public Class<?> getServerComponentClass() {
        return componentClass;
    }

    @Override
    public Component convertToShadedComponent(Object original) {
        if (gsonSerializeMethod == null || gsonComponentSerializer == null || componentClass == null) {
            if (original instanceof Component c) {
                return c;
            }
            throw new IllegalStateException("Adventure component conversion is not available on this server");
        }
        try {
            String json = gsonSerializeMethod.invoke(gsonComponentSerializer, original).toString();
            return GsonComponentSerializer.gson().deserialize(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertToOriginalComponent(String json) {
        if (gsonDeserializeMethod == null || gsonComponentSerializer == null) {
            return null;
        }
        try {
            return gsonDeserializeMethod.invoke(gsonComponentSerializer, json);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Object convertToOriginalComponent(Component component) {
        if (gsonDeserializeMethod == null || gsonComponentSerializer == null) {
            return null;
        }
        try {
            String json = GsonComponentSerializer.gson().serialize(component);
            return gsonDeserializeMethod.invoke(gsonComponentSerializer, json);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
