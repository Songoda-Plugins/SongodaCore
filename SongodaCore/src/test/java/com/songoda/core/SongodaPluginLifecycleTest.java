package com.songoda.core;

import com.songoda.core.testfixture.CoreTestServer;
import net.vortexdevelopment.vinject.annotation.component.Root;
import net.vortexdevelopment.vinject.database.Database;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SongodaPluginLifecycleTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void enableRejectionRunsCleanupOnceAndDoesNotContinueStartup() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TrackingDatabase database = new TrackingDatabase();
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), database);
            plugin.rejectEnable = true;
            plugin.onLoad();
            plugin.onEnable();
            plugin.onDisable();

            assertEquals(1, plugin.enables);
            assertEquals(1, plugin.disables);
            assertEquals(1, database.shutdowns);
            assertEquals(1, plugin.verifications);
            assertEquals(0, plugin.metricsRequests);
            assertFalse(server.messages.toString().contains("Enabled successfully!"));
        }
    }

    @Test
    public void preComponentRejectionStopsBeforeEnableHookAndCleansLoadedPlugin() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TrackingDatabase database = new TrackingDatabase();
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), database);
            plugin.rejectPreComponents = true;
            plugin.onLoad();
            plugin.onEnable();

            assertEquals(1, plugin.preComponents);
            assertEquals(0, plugin.enables);
            assertEquals(1, plugin.disables);
            assertEquals(1, database.shutdowns);
            assertEquals(0, plugin.metricsRequests);
        }
    }

    @Test
    public void loadVerificationFailureDoesNotInvokeUnstartedPluginCleanup() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), new TrackingDatabase());
            plugin.rejectVerification = true;
            plugin.onLoad();
            plugin.onEnable();
            assertEquals(0, plugin.enables);
            assertEquals(0, plugin.disables);
        }
    }

    @Test
    public void throwingPluginCleanupStillShutsDownDatabase() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TrackingDatabase database = new TrackingDatabase();
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), database);
            plugin.throwDuringCleanup = true;
            plugin.onLoad();
            plugin.onDisable();
            plugin.onDisable();
            assertEquals(1, plugin.disables);
            assertEquals(1, database.shutdowns);
        }
    }

    private static class TrackingDatabase extends Database {
        private int shutdowns;

        @Override
        public void shutdown() {
            shutdowns++;
            super.shutdown();
        }
    }

    @Test
    public void verificationThatDisablesAndReturnsDoesNotBeginPluginLoad() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), new TrackingDatabase());
            plugin.stopDuringVerification = true;
            plugin.onLoad();
            assertEquals(0, plugin.loads);
            assertEquals(0, plugin.disables);
        }
    }

    @Test
    public void thrownEnableFailureRunsPluginAndDatabaseCleanup() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TrackingDatabase database = new TrackingDatabase();
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), database);
            plugin.throwDuringEnable = true;
            plugin.onLoad();
            plugin.onEnable();
            assertEquals(1, plugin.enables);
            assertEquals(1, plugin.disables);
            assertEquals(1, database.shutdowns);
            assertEquals(0, plugin.metricsRequests);
        }
    }

    @Test
    public void normalDisableAllowsSubsequentEnableAndCleanup() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            TrackingDatabase database = new TrackingDatabase();
            TestPlugin plugin = server.plugin(TestPlugin.class, folder.getRoot().toPath(), database);
            plugin.onLoad();
            plugin.onEnable();
            CoreTestServer.set(JavaPlugin.class, plugin, "isEnabled", false);
            plugin.onDisable();
            CoreTestServer.set(JavaPlugin.class, plugin, "isEnabled", true);
            plugin.onEnable();
            plugin.onDisable();
            assertEquals(2, plugin.enables);
            assertEquals(2, plugin.disables);
            assertEquals(2, database.shutdowns);
        }
    }

    @Root(packageName = "com.songoda.core.testfixture.empty", createInstance = false)
    public static class TestPlugin extends SongodaPlugin {
        public boolean rejectEnable;
        public boolean rejectPreComponents;
        public boolean rejectVerification;
        public boolean stopDuringVerification;
        public boolean throwDuringEnable;
        public boolean throwDuringCleanup;
        public int enables;
        public int disables;
        public int verifications;
        public int metricsRequests;
        public int preComponents;
        public int loads;

        @Override
        protected void verifyLicense() throws PluginVerificationException {
            verifications++;
            if (stopDuringVerification) {
                stopForVerificationFailure("Rejected and returned", null);
            }
            if (rejectVerification) {
                throw new PluginVerificationException("Rejected by test");
            }
        }

        @Override
        public void onPluginLoad() {
            loads++;
        }

        @Override
        public void onPreComponentLoad() {
            preComponents++;
            if (rejectPreComponents) {
                stopForVerificationFailure("Rejected before components", null);
            }
        }

        @Override
        protected void onPluginEnable() {
            enables++;
            if (throwDuringEnable) {
                throw new IllegalStateException("Enable fixture failure");
            }
            if (rejectEnable) {
                stopForVerificationFailure("Rejected during enable", null);
            }
        }

        @Override
        protected void onPluginDisable() {
            disables++;
            if (throwDuringCleanup) {
                throw new IllegalStateException("Cleanup fixture failure");
            }
        }

        @Override
        protected Integer getBstatsPluginId() {
            metricsRequests++;
            return null;
        }
    }
}
