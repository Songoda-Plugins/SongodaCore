package com.songoda.core.spi;

import com.songoda.core.compatibility.ServerProject;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CommandMaps {

    private static volatile CommandMapBridge bridge;

    private CommandMaps() {
    }

    public static void install(CommandMapBridge commandMapBridge) {
        if (!isCompatiblePlatform(commandMapBridge)) {
            throw new IllegalStateException("Incorrect CommandMapBridge for runtime: expected "
                    + "paper-compatible but got " + commandMapBridge.getClass().getName());
        }
        if (bridge != null && !isCompatiblePlatform(bridge)) {
            throw new IllegalStateException("An incompatible CommandMapBridge was already installed: "
                    + bridge.getClass().getName());
        }
        CommandMaps.bridge = commandMapBridge;
    }

    private static boolean isPaperBridge(CommandMapBridge commandMapBridge) {
        return commandMapBridge != null
                && commandMapBridge.getClass().getName().contains(".platform.paper.");
    }

    /**
     * Installs the Paper {@link CommandMapBridge} before Vinject runs {@code @PostConstruct}, so dynamic
     * command registration works while the {@code DependencyContainer} is still being built (ordering-safe for the
     * core plugin). Child plugins that shade this API into their own JAR should still use {@link #register}'s fallback
     * or depend on SongodaCore with {@code provided} scope so only one {@code CommandMaps} class exists.
     */
    /**
     * @return {@code true} if a bridge is installed after this call (including if it was already installed)
     */
    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass) {
        return installEarlyIfAbsent(pluginMainClass, "com.songoda.core");
    }

    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass, String songodaCorePackage) {
        if (bridge != null) {
            return true;
        }
        String base = pluginMainClass.getPackageName();
        ClassLoader loader = pluginMainClass.getClassLoader();
        String[] candidates = bridgeCandidates(base, songodaCorePackage);
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!CommandMapBridge.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method registerBridge = c.getMethod("registerBridge");
                registerBridge.invoke(instance);
                if (bridge != null) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new IllegalStateException("Could not install a compatible Paper CommandMapBridge for runtime "
                + ServerProject.getServerProject());
    }

    private static String[] bridgeCandidates(String base, String songodaCorePackage) {
        return new String[]{
                songodaCorePackage + ".platform.paper.PaperCommandMapBridge",
                base + ".core.platform.paper.PaperCommandMapBridge",
                "com.songoda.core.platform.paper.PaperCommandMapBridge"
        };
    }

    public static void register(Plugin plugin, PluginCommand command) {
        CommandMapBridge b = bridge;
        if (b == null) {
            // Retry: fat jars that relocate songodacore rely on this; onEnable may have run with a different state.
            installEarlyIfAbsent(plugin.getClass());
            b = bridge;
        }
        if (b != null) {
            if (!isCompatiblePlatform(b)) {
                throw new IllegalStateException("Incorrect CommandMapBridge installed for runtime: expected "
                        + "paper-compatible but got " + b.getClass().getName());
            }
            b.register(plugin, command);
            return;
        }
        if (registerViaHostPluginCommandMaps(plugin, command)) {
            return;
        }
        throw new IllegalStateException(
                "CommandMapBridge not installed (use the unified SongodaCore runtime artifact, and avoid shading "
                        + "com.songoda.core into your plugin JAR - use compileOnly/provided for "
                        + "SongodaCore)");
    }

    /**
     * When this API is duplicated across plugin class loaders, {@link #install} ran on SongodaCore's {@code CommandMaps}
     * but the caller uses another copy with a null bridge. Delegate to any enabled plugin whose {@code CommandMaps}
     * has a non-null bridge (the SongodaCore distribution).
     */
    private static boolean registerViaHostPluginCommandMaps(Plugin plugin, PluginCommand command) {
        for (Plugin host : Bukkit.getPluginManager().getPlugins()) {
            if (host == null || !host.isEnabled()) {
                continue;
            }
            ClassLoader cl = host.getClass().getClassLoader();
            try {
                Class<?> mapsClass = loadCommandMapsClass(host, cl);
                if (mapsClass == null) {
                    continue;
                }
                Field bridgeField = mapsClass.getDeclaredField("bridge");
                bridgeField.setAccessible(true);
                Object foreignBridge = bridgeField.get(null);
                if (foreignBridge == null) {
                    continue;
                }
                Method register = mapsClass.getMethod("register", Plugin.class, PluginCommand.class);
                register.invoke(null, plugin, command);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * Resolves {@code CommandMaps} for a plugin class loader: canonical SongodaCore API, or a shaded copy at
     * {@code {pluginMainPackage}.core.spi.CommandMaps} (e.g. VortexVouchers relocates {@code songodacore}).
     */
    private static @Nullable Class<?> loadCommandMapsClass(Plugin host, ClassLoader cl) {
        try {
            return Class.forName("com.songoda.core.spi.CommandMaps", false, cl);
        } catch (ClassNotFoundException ignored) {
        }
        String relocated = host.getClass().getPackageName() + ".core.spi.CommandMaps";
        try {
            return Class.forName(relocated, false, cl);
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private static boolean isCompatiblePlatform(CommandMapBridge commandMapBridge) {
        return ServerProject.isPaperCompatible() && isPaperBridge(commandMapBridge);
    }
}
