package com.songoda.core.testfixture;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.SongodaCore;
import com.songoda.core.command.CommandManager;
import com.songoda.core.hooks.internal.ReloadHookRegistry;
import com.songoda.core.utils.PluginInitState;
import net.vortexdevelopment.vinject.database.Database;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** Small scheduling fixture that invokes real core code without starting a Minecraft server. */
public final class CoreTestServer implements AutoCloseable {

    public final List<Runnable> queued = new ArrayList<>();
    public final List<String> messages = new ArrayList<>();
    public boolean ownerThread;
    public boolean online = true;
    public int cancellations;
    public final Player player;
    public final Server server;
    private final Object previousServer;
    private final Object previousPlugin;
    private final Object previousCorePlugin;

    public CoreTestServer() throws Exception {
        previousServer = read(Bukkit.class, null, "server");
        previousPlugin = read(SongodaPlugin.class, null, "instance");
        previousCorePlugin = read(SongodaCore.class, null, "currentPlugin");
        BukkitTask task = proxy(BukkitTask.class, (object, method, args) -> defaultValue(method.getReturnType()));
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (object, method, args) -> {
            if (method.getName().equals("runTask")) {
                queued.add((Runnable) args[1]);
                return task;
            }
            if (method.getName().equals("runTaskTimer") || method.getName().equals("runTaskTimerAsynchronously")) {
                return task;
            }
            if (method.getName().equals("cancelTasks")) {
                cancellations++;
            }
            return defaultValue(method.getReturnType());
        });
        PluginManager manager = proxy(PluginManager.class, (object, method, args) -> {
            if (method.getName().equals("disablePlugin")) {
                JavaPlugin plugin = (JavaPlugin) args[0];
                set(JavaPlugin.class, plugin, "isEnabled", false);
                plugin.onDisable();
            }
            return defaultValue(method.getReturnType());
        });
        ConsoleCommandSender console = proxy(ConsoleCommandSender.class, (object, method, args) -> {
            if (method.getName().equals("sendMessage")) {
                messages.add(String.valueOf(args[0]));
            }
            return defaultValue(method.getReturnType());
        });
        server = proxy(Server.class, (object, method, args) -> switch (method.getName()) {
            case "getName" -> "Paper";
            case "getVersion" -> "Paper (MC: 1.21.4)";
            case "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "getMinecraftVersion" -> "1.21.4";
            case "getLogger" -> Logger.getLogger("CoreTestServer");
            case "getScheduler" -> scheduler;
            case "getPluginManager" -> manager;
            case "getConsoleSender" -> console;
            case "getOnlinePlayers" -> List.of();
            case "isPrimaryThread" -> ownerThread;
            default -> defaultValue(method.getReturnType());
        });
        player = proxy(Player.class, (object, method, args) -> switch (method.getName()) {
            case "isOnline" -> online;
            case "getUniqueId" -> new UUID(0, 1);
            case "equals" -> object == args[0];
            case "hashCode" -> System.identityHashCode(object);
            default -> defaultValue(method.getReturnType());
        });
        set(Bukkit.class, null, "server", server);
    }

    public <T extends SongodaPlugin> T plugin(Class<T> type, Path folder, Database database) throws Exception {
        T plugin = allocate(type);
        set(JavaPlugin.class, plugin, "server", server);
        set(JavaPlugin.class, plugin, "description", new PluginDescriptionFile("CoreRegression", "1.0", type.getName()));
        set(JavaPlugin.class, plugin, "pluginMeta", proxy(PluginMeta.class, (object, method, args) -> switch (method.getName()) {
            case "getName" -> "CoreRegression";
            case "getVersion" -> "1.0";
            case "getMainClass" -> type.getName();
            case "namespace" -> "coreregression";
            default -> defaultValue(method.getReturnType());
        }));
        set(JavaPlugin.class, plugin, "dataFolder", folder.toFile());
        set(JavaPlugin.class, plugin, "classLoader", type.getClassLoader());
        set(JavaPlugin.class, plugin, "logger", Logger.getLogger("CoreRegression"));
        set(JavaPlugin.class, plugin, "isEnabled", true);
        set(SongodaPlugin.class, plugin, "database", database);
        set(SongodaPlugin.class, plugin, "commandManager", new CommandManager());
        set(SongodaPlugin.class, plugin, "reloadHooks", new ReloadHookRegistry());
        set(SongodaPlugin.class, plugin, "registeredPlaceholderExpansions", new ArrayList<>());
        set(SongodaPlugin.class, plugin, "lock", new Object());
        set(SongodaPlugin.class, plugin, "initState", PluginInitState.NOT_INITIALIZED);
        set(SongodaPlugin.class, null, "instance", plugin);
        return plugin;
    }

    public void runQueued() {
        ownerThread = true;
        try {
            List<Runnable> tasks = List.copyOf(queued);
            queued.clear();
            tasks.forEach(Runnable::run);
        } finally {
            ownerThread = false;
        }
    }

    public static <T> T allocate(Class<T> type) throws Exception {
        // JavaPlugin normally requires a server-owned class loader. Bypass only construction in tests.
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    public static void set(Class<?> type, Object instance, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    public static Object read(Class<?> type, Object instance, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    public static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == float.class) {
            return 0.0F;
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        set(Bukkit.class, null, "server", previousServer);
        set(SongodaPlugin.class, null, "instance", previousPlugin);
        set(SongodaCore.class, null, "currentPlugin", previousCorePlugin);
    }
}
