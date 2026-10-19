package com.songoda.core.compatibility.folia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchedulerUtilsOwnershipTest {

    private final Queue<Boolean> ownershipChecks = new ArrayDeque<>();
    private Field serverField;
    private Object previousServer;
    private Field foliaField;
    private boolean previousFolia;
    private boolean primaryThread;
    private boolean ownershipFailure;
    private boolean regionOwned;
    private int regionChecks;

    @Before
    public void installServer() throws ReflectiveOperationException {
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        serverField.set(null, proxy(Server.class, (instance, method, arguments) -> switch (method.getName()) {
            case "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "getVersion" -> "Paper (MC: 1.21.4)";
            case "isPrimaryThread" -> {
                if (ownershipFailure) {
                    throw new IllegalStateException("Ownership unavailable");
                }
                yield ownershipChecks.isEmpty() ? primaryThread : ownershipChecks.remove();
            }
            case "isOwnedByCurrentRegion" -> {
                regionChecks++;
                if (ownershipFailure) {
                    throw new IllegalStateException("Ownership unavailable");
                }
                yield ownershipChecks.isEmpty() ? regionOwned : ownershipChecks.remove();
            }
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
    public void entityChecksPreservePrimaryThreadRequirement() {
        Item item = inaccessible(Item.class);

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(item));
        primaryThread = true;
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(item));
    }

    @Test
    public void blockChecksPreservePrimaryThreadRequirementWithoutReadingState() {
        Block block = inaccessible(Block.class);

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(block));
        primaryThread = true;
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(block));
    }

    @Test
    public void locationChecksPreservePrimaryThreadRequirement() {
        Location location = location();

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(location));
        primaryThread = true;
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(location));
    }

    @Test
    public void ownershipExceptionFailsClosed() {
        ownershipFailure = true;

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Item.class)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Block.class)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(location()));
    }

    @Test
    public void nullTargetsFailClosedEvenOnPrimaryThread() {
        primaryThread = true;

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion((Entity) null));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion((Block) null));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion((Location) null));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion((Inventory) null));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(null, inaccessible(Block.class)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Item.class), null));
    }

    @Test
    public void locationWithoutWorldFailsClosed() {
        primaryThread = true;

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(new Location(null, 0, 0, 0)));
    }

    @Test
    public void entityInventoryUsesItsHolderWithoutReadingLocationOrContents() {
        primaryThread = true;
        Inventory inventory = inventory(inaccessible(Player.class));

        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(inventory));
        primaryThread = false;
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inventory));
    }

    @Test
    public void blockInventoryUsesItsBlockLocation() {
        primaryThread = true;
        Chest chest = proxy(Chest.class, (instance, method, arguments) -> {
            if (method.getName().equals("getLocation")) {
                return location();
            }
            throw new AssertionError("Unexpected block access: " + method.getName());
        });

        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(inventory(chest)));
    }

    @Test
    public void inventoryFallsBackToLocationAndRejectsUnknownOrWorldlessLocation() {
        primaryThread = true;

        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(locatedInventory(location())));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(locatedInventory(null)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(locatedInventory(new Location(null, 0, 0, 0))));
    }

    @Test
    public void inaccessibleInventoryHolderFailsClosed() {
        Inventory inventory = proxy(Inventory.class, (instance, method, arguments) -> {
            throw new IllegalStateException("Foreign inventory");
        });

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inventory));
    }

    @Test
    public void inaccessibleBlockInventoryLocationFailsClosed() {
        Chest chest = proxy(Chest.class, (instance, method, arguments) -> {
            throw new IllegalStateException("Foreign block owner");
        });

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inventory(chest)));
    }

    @Test
    public void combinedOwnershipRequiresBothTargets() {
        Item entity = inaccessible(Item.class);
        Block block = inaccessible(Block.class);
        ownershipChecks.add(true);
        ownershipChecks.add(false);

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(entity, block));
        primaryThread = true;
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(entity, block));
    }

    @Test
    public void foliaUsesNativeOwnershipRatherThanPrimaryThread() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        regionOwned = true;

        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Item.class)));
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(locatedBlock()));
        assertTrue(SchedulerUtils.isOwnedByCurrentRegion(location()));
        assertEquals(3, regionChecks);

        primaryThread = true;
        regionOwned = false;
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Item.class)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(locatedBlock()));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(location()));
    }

    @Test
    public void foliaOwnershipExceptionsFailClosed() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        ownershipFailure = true;

        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(inaccessible(Item.class)));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(locatedBlock()));
        assertFalse(SchedulerUtils.isOwnedByCurrentRegion(location()));
        assertEquals(3, regionChecks);
    }

    @Test
    public void requireMethodsKeepTheirActionableFailureMessage() {
        assertOwnershipRequired(() -> SchedulerUtils.requireOwnedByCurrentRegion(location(), "Read location"), "Read location");
        assertOwnershipRequired(() -> SchedulerUtils.requireOwnedByCurrentRegion(inaccessible(Block.class), "Read block"), "Read block");
        ownershipFailure = true;
        assertOwnershipRequired(() -> SchedulerUtils.requireOwnedByCurrentRegion(inaccessible(Item.class), "Read entity"), "Read entity");
    }

    private void assertOwnershipRequired(Runnable action, String description) {
        try {
            action.run();
            fail("Expected ownership to be required");
        } catch (IllegalStateException exception) {
            assertEquals(description + " must run on the primary thread or owning Folia region.", exception.getMessage());
        }
    }

    private Inventory inventory(InventoryHolder holder) {
        return proxy(Inventory.class, (instance, method, arguments) -> {
            if (method.getName().equals("getHolder")) {
                return holder;
            }
            throw new AssertionError("Unexpected inventory access: " + method.getName());
        });
    }

    private Inventory locatedInventory(Location location) {
        return proxy(Inventory.class, (instance, method, arguments) -> switch (method.getName()) {
            case "getHolder" -> null;
            case "getLocation" -> location;
            default -> throw new AssertionError("Unexpected inventory access: " + method.getName());
        });
    }

    private Location location() {
        return new Location(inaccessible(World.class), 0, 0, 0);
    }

    private Block locatedBlock() {
        return proxy(Block.class, (instance, method, arguments) -> {
            if (method.getName().equals("getLocation")) {
                return location();
            }
            throw new AssertionError("Unexpected block access: " + method.getName());
        });
    }

    private <T> T inaccessible(Class<T> type) {
        return proxy(type, (instance, method, arguments) -> {
            throw new AssertionError("Unexpected " + type.getSimpleName() + " access: " + method.getName());
        });
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
