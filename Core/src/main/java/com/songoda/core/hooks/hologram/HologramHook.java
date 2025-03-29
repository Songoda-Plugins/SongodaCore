package com.songoda.core.hooks.hologram;

import com.songoda.core.hooks.Hook;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class HologramHook implements Hook {
    public abstract boolean exists(@NotNull String id);

    /**
     * @throws IllegalStateException if the hologram already exists
     * @see #createOrUpdateText(String, Location, List)
     */
    public abstract void create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines);

    /**
     * @throws IllegalStateException if the hologram does not exist
     */
    public abstract void update(@NotNull String id, @NotNull List<String> lines);

    public abstract void updateBulk(@NotNull Map<String, List<String>> hologramData);

    public abstract void remove(@NotNull String id);

    public abstract void removeAll();

    /**
     * @see #create(String, Location, List)
     */
    public void create(@NotNull String id, @NotNull Location location, @NotNull String text) {
        create(id, location, Collections.singletonList(text));
    }

    public void createOrUpdateText(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        if (exists(id)) {
            update(id, lines);
            return;
        }

        create(id, location, lines);
    }

    /**
     * @see #createOrUpdateText(String, Location, List)
     */
    public void createOrUpdateText(@NotNull String id, @NotNull Location location, @NotNull String text) {
        createOrUpdateText(id, location, Collections.singletonList(text));
    }

    /**
     * @see #update(String, List)
     */
    public void update(@NotNull String id, @NotNull String text) {
        update(id, Collections.singletonList(text));
    }

    protected double getYOffset() {
        return 1.5;
    }

    protected @NotNull Location getNormalizedLocation(Location location) {
        return new Location(
                location.getWorld(),
                location.getX() + (location.getX() - (int) location.getX()) + 0.5,
                location.getY() + (location.getY() - (int) location.getY()) + 0.5 + getYOffset(),
                location.getZ() + (location.getZ() - (int) location.getZ()) + 0.5
        );
    }
}
