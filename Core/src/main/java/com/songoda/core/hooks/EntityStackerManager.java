package com.songoda.core.hooks;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.stackers.Stacker;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * A convenience class for static access to a Stacker HookManager
 *
 * @deprecated This class is part of the old hook system and will be deleted very soon – See {@link SongodaPlugin#getHookManager()}
 */
@Deprecated
public class EntityStackerManager {
    private static final HookManager<Stacker> manager = new HookManager(Stacker.class);

    /**
     * Load all supported economy plugins. <br />
     * Note: This method should be called in your plugin's onEnable() section
     */
    public static void load() {
        manager.load();
    }

    public static HookManager getManager() {
        return manager;
    }

    /**
     * Check to see if there is a default stacker hook loaded. <br />
     * NOTE: using a default stacker assumes that this library is shaded
     *
     * @return returns false if there are no supported plugins
     */
    public static boolean isEnabled() {
        return manager.isEnabled();
    }

    /**
     * Grab the default hologram plugin. <br />
     * NOTE: using a default hologram assumes that this library is shaded
     *
     * @return returns null if no plugin enabled
     */
    public static Stacker getStacker() {
        return manager.getCurrentHook();
    }

    public static boolean isStacked(LivingEntity entity) {
        return manager.isEnabled() && manager.getCurrentHook().isStacked(entity);
    }

    public static int getSize(LivingEntity entity) {
        return manager.isEnabled() ? manager.getCurrentHook().getSize(entity) : 1;
    }

    public static void removeOne(LivingEntity entity) {
        remove(entity, 1);
    }

    public static void remove(LivingEntity entity, int amount) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().remove(entity, amount);
        }
    }

    public static void addOne(LivingEntity entity) {
        add(entity, 1);
    }

    public static void add(LivingEntity entity, int amount) {
        if (manager.isEnabled()) {
            manager.getCurrentHook().add(entity, amount);
        }
    }

    public static int getMinStackSize(EntityType type) {
        return manager.isEnabled() ? manager.getCurrentHook().getMinStackSize(type) : 1;
    }
}
