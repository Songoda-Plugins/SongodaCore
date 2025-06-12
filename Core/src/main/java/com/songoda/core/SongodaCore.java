package com.songoda.core;

import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.ClientVersion;
import com.songoda.core.core.PluginInfo;
import com.songoda.core.core.SongodaCoreCommand;
import com.songoda.core.core.SongodaCoreDiagCommand;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class SongodaCore {
    private static final CoreLogger LOGGER = CoreLogger.getInstance();

    /**
     * Whenever we make a major change to the core GUI, updater,
     * or other function used by the core, increment this number
     *
     * @deprecated The Core's version should be used instead as it uses Semantic Versioning
     */
    @Deprecated
    private static final int coreRevision = 11;

    /**
     * @since coreRevision 6
     * @deprecated Is being replaced by {@link SongodaCoreConstants#getCoreVersion()} which is automatically kept up to date.
     */
    @Deprecated
    private static final String coreVersion = SongodaCoreConstants.getCoreVersion();

    /**
     * This is specific to the website api
     *
     * @deprecated Seems useless and will probably be removed in the near future
     */
    @Deprecated
    private static final int updaterVersion = 1;

    private static final Set<PluginInfo> registeredPlugins = new HashSet<>();

    private static SongodaCore INSTANCE = null;

    private JavaPlugin piggybackedPlugin;
    private CommandManager commandManager;
    private EventListener loginListener;
    private ShadedEventListener shadingListener;

    public static boolean hasShading() {
        // sneaky hack to check the package name since maven tries to re-shade all references to the package string
        return !SongodaCore.class.getPackage().getName().equals(new String(new char[] {'c', 'o', 'm', '.', 'c', 'r', 'a', 'f', 't', 'a', 'r', 'o', '.', 'c', 'o', 'r', 'e'}));
    }

    public static void registerPlugin(JavaPlugin plugin, int pluginID, XMaterial icon) {
        registerPlugin(plugin, pluginID, icon == null ? "STONE" : icon.name(), SongodaCoreConstants.getCoreVersion());
    }

    public static void registerPlugin(JavaPlugin plugin, int pluginID, String icon) {
        registerPlugin(plugin, pluginID, icon, "?");
    }

    public static void registerPlugin(JavaPlugin plugin, int pluginID, String icon, String coreVersion) {
        if (INSTANCE == null) {
            // First: are there any other instances of SongodaCore active?
            for (Class<?> clazz : Bukkit.getServicesManager().getKnownServices()) {
                if (clazz.getSimpleName().equals("SongodaCore")) {
                    try {
                        // test to see if we're up-to-date
                        int otherVersion;
                        int ownVersion;

                        try {
                            otherVersion = (int) clazz.getMethod("getCoreMajorVersion").invoke(null);
                            ownVersion = getCoreMajorVersion();
                        } catch (Exception ignore) {
                            try {
                                otherVersion = (int) clazz.getMethod("getCoreVersion").invoke(null);
                            } catch (Exception ignore2) {
                                otherVersion = -1;
                            }

                            ownVersion = getCoreVersion();
                        }

                        if (otherVersion >= ownVersion) {
                            // use the active service
                            // assuming that the other is greater than R6 if we get here ;)
                            clazz.getMethod("registerPlugin", JavaPlugin.class, int.class, String.class, String.class).invoke(null, plugin, pluginID, icon, coreVersion);

                            if (hasShading()) {
                                (INSTANCE = new SongodaCore()).piggybackedPlugin = plugin;
                                INSTANCE.shadingListener = new ShadedEventListener();
                                Bukkit.getPluginManager().registerEvents(INSTANCE.shadingListener, plugin);
                            }

                            return;
                        }

                        // we are newer than the registered service: steal all of its registrations
                        // grab the old core's registrations
                        List<?> otherPlugins = (List<?>) clazz.getMethod("getPlugins").invoke(null);

                        // destroy the old core
                        Object oldCore = clazz.getMethod("getInstance").invoke(null);
                        Method destruct = clazz.getDeclaredMethod("destroy");
                        destruct.setAccessible(true);
                        destruct.invoke(oldCore);

                        // register ourselves as the SongodaCore service!
                        INSTANCE = new SongodaCore(plugin);
                        INSTANCE.init();
                        INSTANCE.register(plugin, pluginID, icon, coreVersion);
                        Bukkit.getServicesManager().register(SongodaCore.class, INSTANCE, plugin, ServicePriority.Normal);

                        // we need (JavaPlugin plugin, int pluginID, String icon) for our object
                        if (!otherPlugins.isEmpty()) {
                            Object testSubject = otherPlugins.get(0);
                            Class otherPluginInfo = testSubject.getClass();
                            Method otherPluginInfo_getJavaPlugin = otherPluginInfo.getMethod("getJavaPlugin");
                            Method otherPluginInfo_getSongodaId = otherPluginInfo.getMethod("getSongodaId");
                            Method otherPluginInfo_getCoreIcon = otherPluginInfo.getMethod("getCoreIcon");
                            Method otherPluginInfo_getCoreLibraryVersion = otherVersion >= 6 ? otherPluginInfo.getMethod("getCoreLibraryVersion") : null;

                            for (Object other : otherPlugins) {
                                INSTANCE.register(
                                        (JavaPlugin) otherPluginInfo_getJavaPlugin.invoke(other),
                                        (int) otherPluginInfo_getSongodaId.invoke(other),
                                        (String) otherPluginInfo_getCoreIcon.invoke(other),
                                        otherPluginInfo_getCoreLibraryVersion != null ? (String) otherPluginInfo_getCoreLibraryVersion.invoke(other) : "?");
                            }
                        }

                        return;
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                        plugin.getLogger().log(Level.WARNING, "Error registering core service", ex);
                    }
                }
            }

            // register ourselves as the SongodaCore service!
            INSTANCE = new SongodaCore(plugin);
            INSTANCE.init();
            Bukkit.getServicesManager().register(SongodaCore.class, INSTANCE, plugin, ServicePriority.Normal);
        }

        INSTANCE.register(plugin, pluginID, icon, coreVersion);
    }

    SongodaCore() {
        this.commandManager = null;
    }

    SongodaCore(JavaPlugin javaPlugin) {
        this.piggybackedPlugin = javaPlugin;
        this.commandManager = new CommandManager(this.piggybackedPlugin);
        this.loginListener = new EventListener();
    }

    private void init() {
        this.shadingListener = new ShadedEventListener();
        this.commandManager.registerCommandDynamically(new SongodaCoreCommand())
                .addSubCommands(new SongodaCoreDiagCommand());
        Bukkit.getPluginManager().registerEvents(this.loginListener, this.piggybackedPlugin);
        Bukkit.getPluginManager().registerEvents(this.shadingListener, this.piggybackedPlugin);

        // we aggressively want to own this command
        this.tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(this.piggybackedPlugin, () ->
                        CommandManager.registerCommandDynamically(this.piggybackedPlugin, "songoda", this.commandManager, this.commandManager),
                10 * 60));
        this.tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(this.piggybackedPlugin, () ->
                        CommandManager.registerCommandDynamically(this.piggybackedPlugin, "songoda", this.commandManager, this.commandManager),
                20 * 60));
        this.tasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(this.piggybackedPlugin, () ->
                        CommandManager.registerCommandDynamically(this.piggybackedPlugin, "songoda", this.commandManager, this.commandManager),
                20 * 60 * 2));
    }

    /**
     * Used to yield this core to a newer core
     */
    private void destroy() {
        Bukkit.getServicesManager().unregister(SongodaCore.class, INSTANCE);

        this.tasks.stream().filter(Objects::nonNull)
                .forEach(task -> Bukkit.getScheduler().cancelTask(task.getTaskId()));

        HandlerList.unregisterAll(this.loginListener);
        if (!hasShading()) {
            HandlerList.unregisterAll(this.shadingListener);
        }

        registeredPlugins.clear();
        this.commandManager = null;
        this.loginListener = null;
    }

    private final ArrayList<BukkitTask> tasks = new ArrayList<>();

    private void register(JavaPlugin plugin, int pluginID, String icon, String libraryVersion) {
        getLogger().info(getPrefix() + "Hooked " + plugin.getName() + ".");
        PluginInfo info = new PluginInfo(plugin, pluginID, icon, libraryVersion);

        if (plugin.getDescription().getWebsite() != null && plugin.getDescription().getWebsite().contains("songoda.com/")) {
            info.setMarketplaceLink(plugin.getDescription().getWebsite());
        }

        registeredPlugins.add(info);
    }

    public static List<PluginInfo> getPlugins() {
        return new ArrayList<>(registeredPlugins);
    }

    public static String getVersion() {
        return SongodaCoreConstants.getCoreVersion();
    }

    /**
     * @deprecated Use {@link #getCoreMajorVersion()} instead, but careful, coreRevision is at 10 while major version is at 2
     */
    @Deprecated
    public static int getCoreVersion() {
        return coreRevision;
    }

    /**
     * @deprecated Use {@link #getVersion()} instead
     */
    @Deprecated
    public static String getCoreLibraryVersion() {
        return SongodaCoreConstants.getCoreVersion();
    }

    public static int getCoreMajorVersion() {
        String fullVersion = getVersion();
        if (fullVersion.contains(".")) {
            return Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf(".")));
        }

        return -1;
    }

    /**
     * @deprecated Seems useless and will probably be removed in the near future
     */
    @Deprecated
    public static int getUpdaterVersion() {
        return updaterVersion;
    }

    public static String getPrefix() {
        return "[" + SongodaCoreConstants.getProjectName() + "] ";
    }

    public static CoreLogger getLogger() {
        return LOGGER;
    }

    public static boolean isRegistered(String plugin) {
        return registeredPlugins.stream().anyMatch(p -> p.getJavaPlugin().getName().equalsIgnoreCase(plugin));
    }

    public static JavaPlugin getHijackedPlugin() {
        return INSTANCE == null ? null : INSTANCE.piggybackedPlugin;
    }

    public static SongodaCore getInstance() {
        return INSTANCE;
    }

    private static class ShadedEventListener implements Listener {
        boolean via;
        boolean proto = false;

        ShadedEventListener() {
            this.via = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");

            if (this.via) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginVia(p, getHijackedPlugin()));
                return;
            }

            this.proto = Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");
            if (this.proto) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginProtocol(p, getHijackedPlugin()));
            }
        }

        @EventHandler
        void onLogin(PlayerLoginEvent event) {
            if (this.via) {
                ClientVersion.onLoginVia(event.getPlayer(), getHijackedPlugin());
                return;
            }

            if (this.proto) {
                ClientVersion.onLoginProtocol(event.getPlayer(), getHijackedPlugin());
            }
        }

        @EventHandler
        void onLogout(PlayerQuitEvent event) {
            if (this.via) {
                ClientVersion.onLogout(event.getPlayer());
            }
        }

        @EventHandler
        void onEnable(PluginEnableEvent event) {
            // technically shouldn't have online players here, but idk
            if (!this.via && (this.via = event.getPlugin().getName().equals("ViaVersion"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginVia(p, getHijackedPlugin()));
            } else if (!this.proto && (this.proto = event.getPlugin().getName().equals("ProtocolSupport"))) {
                Bukkit.getOnlinePlayers().forEach(p -> ClientVersion.onLoginProtocol(p, getHijackedPlugin()));
            }
        }
    }

    private class EventListener implements Listener {
        @EventHandler
        void onDisable(PluginDisableEvent event) {
            // don't track disabled plugins
            PluginInfo pi = registeredPlugins.stream().filter(p -> event.getPlugin() == p.getJavaPlugin()).findFirst().orElse(null);

            if (pi != null) {
                registeredPlugins.remove(pi);
            }

            if (event.getPlugin() == SongodaCore.this.piggybackedPlugin) {
                // uh-oh! Abandon ship!!
                Bukkit.getServicesManager().unregisterAll(SongodaCore.this.piggybackedPlugin);

                // can we move somewhere else?
                if ((pi = registeredPlugins.stream().findFirst().orElse(null)) != null) {
                    // move ourselves to this plugin
                    SongodaCore.this.piggybackedPlugin = pi.getJavaPlugin();

                    Bukkit.getServicesManager().register(SongodaCore.class, INSTANCE, SongodaCore.this.piggybackedPlugin, ServicePriority.Normal);
                    Bukkit.getPluginManager().registerEvents(SongodaCore.this.loginListener, SongodaCore.this.piggybackedPlugin);
                    Bukkit.getPluginManager().registerEvents(SongodaCore.this.shadingListener, SongodaCore.this.piggybackedPlugin);
                    CommandManager.registerCommandDynamically(SongodaCore.this.piggybackedPlugin, "songoda", SongodaCore.this.commandManager, SongodaCore.this.commandManager);
                }
            }
        }
    }
}
