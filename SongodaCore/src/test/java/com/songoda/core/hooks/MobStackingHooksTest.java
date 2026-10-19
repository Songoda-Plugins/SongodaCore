package com.songoda.core.hooks;

import com.songoda.core.compatibility.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobStackingHooksTest {

    private Field serverField;
    private Object previousServer;
    private Field foliaField;
    private boolean previousFolia;
    private boolean owned = true;
    private Plugin mythicMobs;
    private final FakeMobManager mobManager = new FakeMobManager();

    @Before
    public void installServer() throws ReflectiveOperationException {
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        PluginManager plugins = proxy(PluginManager.class, (instance, method, arguments) -> {
            if (method.getName().equals("getPlugin")) {
                return "MythicMobs".equals(arguments[0]) ? mythicMobs : null;
            }
            throw new AssertionError("Unexpected plugin manager access: " + method.getName());
        });
        serverField.set(null, proxy(Server.class, (instance, method, arguments) -> switch (method.getName()) {
            case "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "getVersion" -> "Paper (MC: 1.21.4)";
            case "isPrimaryThread" -> owned;
            case "getPluginManager" -> plugins;
            default -> throw new AssertionError("Unexpected server access: " + method.getName());
        }));
        foliaField = SchedulerUtils.class.getDeclaredField("isFolia");
        foliaField.setAccessible(true);
        previousFolia = foliaField.getBoolean(null);
        foliaField.setBoolean(null, false);
    }

    @After
    public void restoreServer() throws IllegalAccessException {
        foliaField.setBoolean(null, previousFolia);
        serverField.set(null, previousServer);
    }

    @Test
    public void absentOptionalPluginsAllowStackingWithoutReadingEntityState() {
        MobStackingHooks.registerFlags();
        assertTrue(canStack());
    }

    @Test
    public void foreignEntityFailsClosedBeforeIntegrationOrEntityAccess() {
        owned = false;
        assertFalse(canStack());
    }

    @Test
    public void enabledMythicMobsUsesDirectLookupAndProtectsRecognizedMobs() {
        mythicMobs = mythicPlugin(true);
        assertTrue(canStack());
        mobManager.customMob = new Object();
        assertFalse(canStack());
    }

    @Test
    public void disabledOptionalPluginDoesNotBlockVanillaMobs() {
        mythicMobs = mythicPlugin(false);
        mobManager.customMob = new Object();
        assertTrue(canStack());
    }

    @Test
    public void unavailableMythicApiFailsClosed() {
        mythicMobs = proxy(Plugin.class, (instance, method, arguments) -> {
            if (method.getName().equals("isEnabled")) {
                return true;
            }
            throw new AssertionError("Unexpected plugin access: " + method.getName());
        });
        assertFalse(canStack());
    }

    @Test
    public void configuredBlacklistProtectsCustomMobsEvenWithoutEnabledIntegrationEntry() {
        mythicMobs = mythicPlugin(true);
        mobManager.customMob = new Object();
        assertFalse(MobStackingHooks.canStack(entity(), List.of(), List.of("mythicmobs_test")));
    }

    @Test
    public void unconfiguredIntegrationDoesNotInvokeItsApi() {
        mythicMobs = proxy(Plugin.class, (instance, method, arguments) -> {
            if (method.getName().equals("isEnabled")) {
                return true;
            }
            throw new AssertionError("Unexpected plugin access: " + method.getName());
        });
        assertTrue(MobStackingHooks.canStack(entity(), List.of(), List.of()));
    }

    private boolean canStack() {
        return MobStackingHooks.canStack(entity(), List.of("MythicMobs"), List.of());
    }

    private Plugin mythicPlugin(boolean enabled) {
        return proxy(MythicPlugin.class, (instance, method, arguments) -> switch (method.getName()) {
            case "isEnabled" -> enabled;
            case "getMobManager" -> mobManager;
            default -> throw new AssertionError("Unexpected plugin access: " + method.getName());
        });
    }

    private LivingEntity entity() {
        return proxy(LivingEntity.class, (instance, method, arguments) -> {
            throw new AssertionError("Unexpected entity access: " + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    public interface MythicPlugin extends Plugin {

        FakeMobManager getMobManager();
    }

    public static final class FakeMobManager {

        private Object customMob;

        public Object getMythicMobInstance(Entity entity) {
            return customMob;
        }
    }
}
