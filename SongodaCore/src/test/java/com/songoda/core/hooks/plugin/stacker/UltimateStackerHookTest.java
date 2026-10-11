package com.songoda.core.hooks.plugin.stacker;

import com.songoda.core.hooks.types.StackerHook;
import com.songoda.core.testfixture.CoreTestServer;
import com.songoda.ultimatestacker.api.UltimateStackerApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class UltimateStackerHookTest {

    @Test
    public void exposesStackOperationsWithoutLinkingUltimateStackerTypes() throws Exception {
        UltimateStackerHook hook = new UltimateStackerHook();

        assertEquals("UltimateStacker", hook.getRequiredPlugin());
        assertNotNull(StackerHook.class.getMethod("getRealAmount", org.bukkit.entity.Item.class));
        assertNotNull(StackerHook.class.getMethod("createStackedItem", Location.class, org.bukkit.inventory.ItemStack.class, BigInteger.class));
        assertNotNull(StackerHook.class.getMethod("getSpawnerStackAmount", Location.class));
        assertNotNull(StackerHook.class.getMethod("createStackedSpawner", BlockPlaceEvent.class));
        assertNotNull(StackerHook.class.getMethod("getBlockStackAmount", Location.class));
        assertNotNull(StackerHook.class.getMethod("createStackedBlock", Location.class, Material.class, BigInteger.class));

        for (Method method : UltimateStackerHook.class.getMethods()) {
            assertFalse(method.getDeclaringClass().getName().startsWith("com.songoda.ultimatestacker."));
        }
    }

    @Test
    public void clearsManagersWhenUltimateStackerIsAbsentOrDisabled() throws Exception {
        try (CoreTestServer ignored = new CoreTestServer()) {
            UltimateStackerHook hook = new UltimateStackerHook();
            setManager(hook, "itemManager", new Object());
            setManager(hook, "spawnerManager", new Object());
            setManager(hook, "blockManager", new Object());

            hook.onEnable();

            assertNullManager(hook, "itemManager");
            assertNullManager(hook, "spawnerManager");
            assertNullManager(hook, "blockManager");

            setManager(hook, "itemManager", new Object());
            setManager(hook, "spawnerManager", new Object());
            setManager(hook, "blockManager", new Object());
            hook.onDisable();

            assertNullManager(hook, "itemManager");
            assertNullManager(hook, "spawnerManager");
            assertNullManager(hook, "blockManager");
        }
    }

    @Test
    public void connectsDisconnectsAndReconnectsAcrossPluginLifecycle() throws Exception {
        Object previousServer = CoreTestServer.read(Bukkit.class, null, "server");
        boolean[] enabled = {true};
        Plugin plugin = CoreTestServer.proxy(Plugin.class, (object, method, args) -> null);
        PluginManager pluginManager = CoreTestServer.proxy(PluginManager.class, (object, method, args) -> switch (method.getName()) {
            case "isPluginEnabled" -> enabled[0] && "UltimateStacker".equals(args[0]);
            case "getPlugin" -> "UltimateStacker".equals(args[0]) ? plugin : null;
            default -> null;
        });
        Server server = CoreTestServer.proxy(Server.class, (object, method, args) -> switch (method.getName()) {
            case "getPluginManager" -> pluginManager;
            case "getLogger" -> Logger.getLogger("UltimateStackerHookTest");
            default -> null;
        });

        try {
            CoreTestServer.set(Bukkit.class, null, "server", server);
            UltimateStackerHook hook = new UltimateStackerHook();

            hook.onEnable();
            assertConnected(hook);

            hook.onDisable();
            assertDisconnected(hook);

            enabled[0] = false;
            hook.onEnable();
            assertDisconnected(hook);

            enabled[0] = true;
            hook.onEnable();
            assertConnected(hook);
        } finally {
            CoreTestServer.set(Bukkit.class, null, "server", previousServer);
            UltimateStackerApi.reset();
        }
    }

    private static void setManager(UltimateStackerHook hook, String fieldName, Object value) throws Exception {
        Field field = UltimateStackerHook.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(hook, value);
    }

    private static void assertNullManager(UltimateStackerHook hook, String fieldName) throws Exception {
        Field field = UltimateStackerHook.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        org.junit.Assert.assertNull(field.get(hook));
    }

    private static void assertConnected(UltimateStackerHook hook) throws Exception {
        assertNotNull(manager(hook, "itemManager"));
        assertNotNull(manager(hook, "spawnerManager"));
        assertNotNull(manager(hook, "blockManager"));
    }

    private static void assertDisconnected(UltimateStackerHook hook) throws Exception {
        assertNullManager(hook, "itemManager");
        assertNullManager(hook, "spawnerManager");
        assertNullManager(hook, "blockManager");
    }

    private static Object manager(UltimateStackerHook hook, String fieldName) throws Exception {
        Field field = UltimateStackerHook.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(hook);
    }
}
