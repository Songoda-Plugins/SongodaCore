package com.songoda.core.configuration;

import com.songoda.core.SongodaCore;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.cryptomorin.xseries.XMaterial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class ConfigSetting {
    final Config config;
    final String key;

    public ConfigSetting(@NotNull Config config, @NotNull String key) {
        this.config = config;
        this.key = key;
    }

    public ConfigSetting(@NotNull Config config, @NotNull String key, @NotNull Object defaultValue, String... comment) {
        this.config = config;
        this.key = key;

        config.setDefault(key, defaultValue, comment);
    }

    public ConfigSetting(@NotNull Config config, @NotNull String key, @NotNull Object defaultValue, ConfigFormattingRules.CommentStyle commentStyle, String... comment) {
        this.config = config;
        this.key = key;

        config.setDefault(key, defaultValue, commentStyle, comment);
    }

    @NotNull
    public String getKey() {
        return this.key;
    }

    public List<Integer> getIntegerList() {
        return this.config.getIntegerList(this.key);
    }

    public List<String> getStringList() {
        return this.config.getStringList(this.key);
    }

    public boolean getBoolean() {
        return this.config.getBoolean(this.key);
    }

    public boolean getBoolean(boolean def) {
        return this.config.getBoolean(this.key, def);
    }

    public int getInt() {
        return this.config.getInt(this.key);
    }

    public int getInt(int def) {
        return this.config.getInt(this.key, def);
    }

    public long getLong() {
        return this.config.getLong(this.key);
    }

    public long getLong(long def) {
        return this.config.getLong(this.key, def);
    }

    public double getDouble() {
        return this.config.getDouble(this.key);
    }

    public double getDouble(double def) {
        return this.config.getDouble(this.key, def);
    }

    public String getString() {
        return this.config.getString(this.key);
    }

    public String getString(String def) {
        return this.config.getString(this.key, def);
    }

    public Object getObject() {
        return this.config.get(this.key);
    }

    public Object getObject(Object def) {
        return this.config.get(this.key, def);
    }

    public <T> T getObject(@NotNull Class<T> clazz) {
        return this.config.getObject(this.key, clazz);
    }

    public <T> T getObject(@NotNull Class<T> clazz, @Nullable T def) {
        return this.config.getObject(this.key, clazz, def);
    }

    public char getChar() {
        return this.config.getChar(this.key);
    }

    public char getChar(char def) {
        return this.config.getChar(this.key, def);
    }

    @NotNull
    public XMaterial getMaterial() {
        String val = this.config.getString(this.key);
        Optional<XMaterial> mat = CompatibleMaterial.getMaterial(this.config.getString(this.key));

        if (!mat.isPresent()) {
            SongodaCore.getLogger().log(Level.WARNING, String.format("Config value \"%s\" has an invalid material name: \"%s\"", this.key, val));
        }
        return mat.orElse(XMaterial.STONE);
    }

    @NotNull
    public XMaterial getMaterial(@NotNull XMaterial def) {
        //return config.getMaterial(key, def);
        String val = this.config.getString(this.key);
        Optional<XMaterial> mat = val != null ? CompatibleMaterial.getMaterial(val) : Optional.empty();

        if (!mat.isPresent()) {
            SongodaCore.getLogger().log(Level.WARNING, String.format("Config value \"%s\" has an invalid material name: \"%s\"", this.key, val));
        }
        return mat.orElse(def);
    }
}
