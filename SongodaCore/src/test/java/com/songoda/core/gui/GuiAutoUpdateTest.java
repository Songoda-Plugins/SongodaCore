package com.songoda.core.gui;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.SongodaPluginLifecycleTest;
import com.songoda.core.compatibility.folia.FoliaDelegate;
import com.songoda.core.compatibility.folia.SchedulerUtils;
import com.songoda.core.testfixture.CoreTestServer;
import net.vortexdevelopment.vinject.database.Database;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GuiAutoUpdateTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsLatestItemsOnlyInViewerTaskAndCoalescesPendingUpdates() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            server.plugin(SongodaPluginLifecycleTest.TestPlugin.class, folder.getRoot().toPath(), new Database());
            TrackingGui gui = CoreTestServer.allocate(TrackingGui.class);
            gui.server = server;
            List<GuiItem> items = new ArrayList<>();
            CountingItem removed = new CountingItem(0, 0);
            CountingItem added = new CountingItem(1, 0);
            items.add(removed);
            initialize(gui, Gui.class, server, items);

            gui.autoUpdate();
            gui.autoUpdate();
            assertEquals(0, gui.reads);
            assertEquals(1, server.queued.size());
            items.clear();
            items.add(added);
            server.runQueued();
            assertEquals(1, gui.reads);
            assertEquals(0, removed.updates);
            assertEquals(1, added.updates);

            gui.autoUpdate();
            server.runQueued();
            assertEquals(2, added.updates);
        }
    }

    @Test
    public void closedOrDisabledMenuDoesNotReadOrUpdateItems() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            SongodaPlugin plugin = server.plugin(SongodaPluginLifecycleTest.TestPlugin.class, folder.getRoot().toPath(), new Database());
            TrackingGui gui = CoreTestServer.allocate(TrackingGui.class);
            gui.server = server;
            List<Player> viewers = initialize(gui, Gui.class, server, new ArrayList<>());
            gui.autoUpdate();
            viewers.clear();
            server.runQueued();
            assertEquals(0, gui.reads);

            viewers.add(server.player);
            gui.autoUpdate();
            CoreTestServer.set(JavaPlugin.class, plugin, "isEnabled", false);
            server.runQueued();
            assertEquals(0, gui.reads);
        }
    }

    @Test
    public void paginationReadsCurrentPageOnOwnerAndWritesUpdatedPageAndStaticItems() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            server.plugin(SongodaPluginLifecycleTest.TestPlugin.class, folder.getRoot().toPath(), new Database());
            PaginatedGui gui = CoreTestServer.allocate(PaginatedGui.class);
            initialize(gui, PaginatedGui.class, server, null);
            CountingItem oldPage = new CountingItem(-1, -1);
            CountingItem currentPage = new CountingItem(-1, -1);
            CountingItem staticItem = new CountingItem(8, 1);
            Map<Integer, List<GuiItem>> pages = new HashMap<>();
            pages.put(0, new ArrayList<>(List.of(oldPage)));
            CoreTestServer.set(PaginatedGui.class, gui, "pages", pages);
            CoreTestServer.set(PaginatedGui.class, gui, "staticItems", new ArrayList<>(List.of(staticItem)));
            CoreTestServer.set(PaginatedGui.class, gui, "rows", 2);
            CoreTestServer.set(PaginatedGui.class, gui, "itemsPerPage", 9);
            CoreTestServer.set(PaginatedGui.class, gui, "pageIndicatorSlot", -1);
            List<Integer> slots = new ArrayList<>();
            CoreTestServer.set(PaginatedGui.class, gui, "inventory", inventory(server, slots));

            gui.autoUpdate();
            assertEquals(0, oldPage.updates);
            pages.clear();
            pages.put(1, new ArrayList<>(List.of(currentPage)));
            CoreTestServer.set(PaginatedGui.class, gui, "currentPage", 1);
            server.runQueued();
            assertEquals(0, oldPage.updates);
            assertEquals(1, currentPage.updates);
            assertEquals(1, staticItem.updates);
            assertTrue(slots.contains(0));
            assertTrue(slots.contains(17));
        }
    }

    private static List<Player> initialize(Object gui, Class<?> type, CoreTestServer server, List<GuiItem> items) throws Exception {
        List<Player> viewers = new CopyOnWriteArrayList<>(List.of(server.player));
        CoreTestServer.set(type, gui, "openers", viewers);
        CoreTestServer.set(type, gui, "autoUpdatePending", new AtomicBoolean());
        CoreTestServer.set(type, gui, "inventory", inventory(server, new ArrayList<>()));
        if (items != null) {
            CoreTestServer.set(type, gui, "items", items);
        }
        return viewers;
    }

    @Test
    public void foliaUsesEntitySchedulerAndRetirementReleasesPendingUpdate() throws Exception {
        try (CoreTestServer server = new CoreTestServer()) {
            server.plugin(SongodaPluginLifecycleTest.TestPlugin.class, folder.getRoot().toPath(), new Database());
            Object previousFolia = CoreTestServer.read(SchedulerUtils.class, null, "isFolia");
            Object previousDelegate = CoreTestServer.read(SchedulerUtils.class, null, "foliaDelegate");
            List<Runnable> retired = new ArrayList<>();
            BukkitTask task = CoreTestServer.proxy(BukkitTask.class, (object, method, args) -> null);
            FoliaDelegate delegate = CoreTestServer.proxy(FoliaDelegate.class, (object, method, args) -> {
                assertEquals("runEntity", method.getName());
                assertSame(server.player, args[1]);
                server.queued.add((Runnable) args[2]);
                retired.add((Runnable) args[3]);
                return task;
            });
            try {
                CoreTestServer.set(SchedulerUtils.class, null, "isFolia", true);
                CoreTestServer.set(SchedulerUtils.class, null, "foliaDelegate", delegate);
                TrackingGui gui = CoreTestServer.allocate(TrackingGui.class);
                gui.server = server;
                CountingItem item = new CountingItem(0, 0);
                initialize(gui, Gui.class, server, new ArrayList<>(List.of(item)));

                gui.autoUpdate();
                assertEquals(0, gui.reads);
                server.queued.clear();
                retired.get(0).run();
                gui.autoUpdate();
                assertEquals(1, server.queued.size());
                server.runQueued();
                assertEquals(1, gui.reads);
                assertEquals(1, item.updates);
            } finally {
                CoreTestServer.set(SchedulerUtils.class, null, "isFolia", previousFolia);
                CoreTestServer.set(SchedulerUtils.class, null, "foliaDelegate", previousDelegate);
            }
        }
    }

    private static Inventory inventory(CoreTestServer server, List<Integer> slots) {
        return CoreTestServer.proxy(Inventory.class, (object, method, args) -> {
            if (method.getName().equals("setItem")) {
                assertTrue("Inventory writes must run in the viewer task", server.ownerThread);
                slots.add((Integer) args[0]);
            }
            return null;
        });
    }

    private static class TrackingGui extends Gui {
        private CoreTestServer server;
        private int reads;

        private TrackingGui() {
            super("fixture", 1);
        }

        @Override
        public List<GuiItem> getItems() {
            assertTrue("Item reads must run in the viewer task", server.ownerThread);
            reads++;
            return super.getItems();
        }
    }

    private static class CountingItem extends GuiItem {
        private int updates;

        private CountingItem(int x, int y) {
            super((ItemStack) null, x, y);
        }

        @Override
        public State getState() {
            return State.AUTO_UPDATE;
        }

        @Override
        ItemStack updateItem() {
            updates++;
            return null;
        }
    }
}
