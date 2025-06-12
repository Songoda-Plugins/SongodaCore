package com.songoda.core.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Used to easily store a set of one data value
 *
 * @param <T> DataObject class that is used to store the data
 */
public class SimpleDataStore<T extends DataStoreObject> {
    protected final Plugin plugin;
    protected final String filename, dirName;
    private final Function<ConfigurationSection, T> getFromSection;
    protected final HashMap<Object, T> data = new HashMap<>();
    private File file;
    private final Object lock = new Object();
    SaveTask saveTask;
    Timer autosaveTimer;
    /**
     * time in seconds to start a save after a change is made
     */
    int autosaveInterval = 60;

    public SimpleDataStore(@NotNull Plugin plugin, @NotNull String filename, @NotNull Function<ConfigurationSection, T> loadFunction) {
        this.plugin = plugin;
        this.filename = filename;
        this.dirName = null;
        this.getFromSection = loadFunction;
    }

    public SimpleDataStore(@NotNull Plugin plugin, @Nullable String directory, @NotNull String filename, @NotNull Function<ConfigurationSection, T> loadFunction) {
        this.plugin = plugin;
        this.filename = filename;
        this.dirName = directory;
        this.getFromSection = loadFunction;
    }

    @NotNull
    public File getFile() {
        if (this.file == null) {
            if (this.dirName != null) {
                this.file = new File(this.plugin.getDataFolder() + this.dirName, this.filename != null ? this.filename : "data.yml");
            } else {
                this.file = new File(this.plugin.getDataFolder(), this.filename != null ? this.filename : "data.yml");
            }
        }

        return this.file;
    }

    /**
     * @return a directly modifiable instance of the data mapping for this storage
     */
    public Map<Object, T> getData() {
        return this.data;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * @param key key whose mapping is to be retrieved from this storage
     *
     * @return the value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    @Nullable
    public T get(Object key) {
        return this.data.get(key);
    }

    /**
     * Removes the mapping for the specified key from this storage if present.
     *
     * @param key key whose mapping is to be removed from this storage
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    @Nullable
    public T remove(@NotNull Object key) {
        T temp;

        synchronized (this.lock) {
            temp = this.data.remove(key);
        }

        save();

        return temp;
    }

    /**
     * Removes the mapping for the specified key from this storage if present.
     *
     * @param value value whose mapping is to be removed from this storage
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    @Nullable
    public T remove(@NotNull T value) {
        if (value == null) {
            return null;
        }

        T temp;

        synchronized (this.lock) {
            temp = this.data.remove(value.getKey());
        }

        save();

        return temp;
    }

    /**
     * Adds the specified value in this storage. If the map previously contained
     * a mapping for the key, the old value is replaced.
     *
     * @param value value to be added
     *
     * @return the previous value associated with <tt>value.getKey()</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>value.getKey()</tt>.
     */
    @Nullable
    public T add(@NotNull T value) {
        if (value == null) {
            return null;
        }

        T temp;

        synchronized (this.lock) {
            temp = this.data.put(value.getKey(), value);
        }

        save();

        return temp;
    }

    /**
     * Adds the specified value in this storage. If the map previously contained
     * a mapping for the key, the old value is replaced.
     *
     * @param value values to be added
     */
    public void addAll(@NotNull T[] value) {
        if (value == null) {
            return;
        }

        synchronized (this.lock) {
            for (T t : value) {
                if (t != null) {
                    this.data.put(t.getKey(), t);
                }
            }
        }

        save();
    }

    /**
     * Adds the specified value in this storage. If the map previously contained
     * a mapping for the key, the old value is replaced.
     *
     * @param value values to be added
     */
    @Nullable
    public void addAll(@NotNull Collection<T> value) {
        if (value == null) {
            return;
        }

        synchronized (this.lock) {
            for (T v : value) {
                if (v != null) {
                    this.data.put(v.getKey(), v);
                }
            }
        }

        save();
    }

    /**
     * Load data from the associated file
     */
    public void load() {
        if (!getFile().exists()) {
            return;
        }

        try {
            YamlConfiguration f = new YamlConfiguration();
            f.options().pathSeparator('\0');
            f.load(this.file);

            synchronized (this.lock) {
                this.data.clear();

                f.getValues(false).values().stream()
                        .filter(ConfigurationSection.class::isInstance)
                        .map(v -> this.getFromSection.apply((ConfigurationSection) v))
                        .forEach(v -> this.data.put(v.getKey(), v));
            }
        } catch (IOException | InvalidConfigurationException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load data from " + this.file.getName(), ex);
        }
    }

    /**
     * Optionally, save this storage's data to file if there have been changes
     * made
     */
    public void saveChanges() {
        if (this.saveTask != null || this.data.values().stream().anyMatch(DataStoreObject::hasChanged)) {
            flushSave();
        }
    }

    /**
     * Save this file data. This saves later asynchronously.
     */
    public void save() {
        // save async even if no plugin or if plugin disabled
        if (this.saveTask == null) {
            this.autosaveTimer = new Timer((this.plugin != null ? this.plugin.getName() + "-DataStoreSave-" : "DataStoreSave-") + getFile().getName());
            this.autosaveTimer.schedule(this.saveTask = new SaveTask(), this.autosaveInterval * 1000L);
        }
    }

    /**
     * Force a new save of this storage's data
     */
    public void flushSave() {
        if (this.saveTask != null) {
            //Close Threads
            this.saveTask.cancel();
            this.autosaveTimer.cancel();
            this.saveTask = null;
            this.autosaveTimer = null;
        }

        YamlConfiguration yamlConfig = new YamlConfiguration();

        synchronized (this.lock) {
            this.data.values().forEach(e -> e.saveToSection(yamlConfig.createSection(e.getConfigKey())));
        }

        try {
            yamlConfig.save(getFile());
            this.data.values().forEach(e -> e.setChanged(false));
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save data to " + this.file.getName(), ex);
        }
    }

    class SaveTask extends TimerTask {
        @Override
        public void run() {
            flushSave();
        }
    }
}
