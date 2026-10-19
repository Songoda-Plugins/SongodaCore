package com.songoda.core.hooks;

import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.hooks.protection.WorldGuardMobStackingHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

/**
 * Optional integrations that protect region flags and custom mobs during mob stacking.
 */
public final class MobStackingHooks {

    private static final ClassValue<Optional<ManagerAccess>> MANAGER_ACCESS = new ClassValue<>() {
        @Override
        protected Optional<ManagerAccess> computeValue(Class<?> type) {
            try {
                return Optional.of(new ManagerAccess(type.getMethod("getMobManager"), null));
            } catch (NoSuchMethodException ignored) {
                try {
                    Field field = type.getDeclaredField("mobManager");
                    field.setAccessible(true);
                    return Optional.of(new ManagerAccess(null, field));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return Optional.empty();
                }
            }
        }
    };

    private static final ClassValue<Optional<Method>> MOB_LOOKUP = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("getMythicMobInstance", Entity.class));
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        }
    };

    private MobStackingHooks() {
    }

    /**
     * Registers the default-allow WorldGuard mob-stacking flag during the consuming plugin's load phase.
     * Does nothing when WorldGuard is not installed.
     */
    public static void registerFlags() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardMobStackingHook.registerFlag();
        }
    }

    private static volatile boolean checkedPlugins = false;
    private static volatile boolean worldGuardInstalled = false;
    private static volatile boolean mythicMobsInstalled = false;
    private static volatile long lastPluginCheckTime = 0L;

    private static void refreshPlugins() {
        long now = System.currentTimeMillis();
        if (checkedPlugins && now - lastPluginCheckTime < 10000L) {
            return;
        }
        Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
        worldGuardInstalled = wg != null && wg.isEnabled();
        Plugin mm = Bukkit.getPluginManager().getPlugin("MythicMobs");
        mythicMobsInstalled = mm != null && mm.isEnabled();
        lastPluginCheckTime = now;
        checkedPlugins = true;
    }

    /**
     * Checks optional stacking protections on the entity's owning region.
     * Recognized custom mobs, including blacklisted IDs, are excluded so merging cannot destroy their custom state.
     * Missing integrations impose no restriction; a present provider that cannot be queried fails closed.
     *
     * @param entity the owned entity to check
     * @param enabledCustomEntityPlugins custom-mob integrations enabled by the consuming plugin
     * @param blacklistedCustomEntities provider-prefixed custom IDs which must never stack
     * @return whether optional integrations permit stacking this entity
     */
    public static boolean canStack(@NotNull LivingEntity entity, @NotNull Collection<String> enabledCustomEntityPlugins, @NotNull Collection<String> blacklistedCustomEntities) {
        if (!SchedulerUtils.isOwnedByCurrentRegion(entity)) {
            return false;
        }

        refreshPlugins();

        if (worldGuardInstalled) {
            try {
                if (!WorldGuardMobStackingHook.isAllowed(entity.getLocation())) {
                    return false;
                }
            } catch (RuntimeException | LinkageError exception) {
                return false;
            }
        }

        if (!mythicMobsInstalled) {
            return true;
        }

        boolean mythicMobsEnabled = false;
        for (String p : enabledCustomEntityPlugins) {
            if ("MythicMobs".equalsIgnoreCase(p)) {
                mythicMobsEnabled = true;
                break;
            }
        }
        boolean mythicMobsBlacklisted = false;
        for (String id : blacklistedCustomEntities) {
            if (id != null && id.regionMatches(true, 0, "mythicmobs_", 0, "mythicmobs_".length())) {
                mythicMobsBlacklisted = true;
                break;
            }
        }
        if (!mythicMobsEnabled && !mythicMobsBlacklisted) {
            return true;
        }

        try {
            Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicMobs == null || !mythicMobs.isEnabled()) {
                return true;
            }
            Object manager = getMythicMobManager(mythicMobs);
            Optional<Method> lookup = MOB_LOOKUP.get(manager.getClass());
            return lookup.isPresent() && lookup.get().invoke(manager, entity) == null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static Object getMythicMobManager(Plugin plugin) throws ReflectiveOperationException {
        ManagerAccess access = MANAGER_ACCESS.get(plugin.getClass()).orElseThrow(NoSuchMethodException::new);
        return access.getter() == null ? access.field().get(plugin) : access.getter().invoke(plugin);
    }

    private record ManagerAccess(Method getter, Field field) {
    }
}
