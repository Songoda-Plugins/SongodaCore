package com.songoda.core.world;

import com.songoda.core.compatibility.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class NearbyOwnedEntitiesTest {

    private Object previousServer;
    private boolean previousFolia;
    private Field serverField;
    private Field foliaField;
    private Entity origin;
    private Entity owned;
    private int nativeQueries;
    private int chunkQueries;

    @Before
    public void installServer() throws ReflectiveOperationException {
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        serverField.set(null, proxy(Server.class, (instance, method, args) -> switch (method.getName()) {
            case "isPrimaryThread" -> true;
            case "getVersion", "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "isOwnedByCurrentRegion" -> args[0] instanceof Entity entity
                    ? entity == origin || entity == owned
                    : args[0] instanceof Location location
                    ? location.getBlockX() >> 4 == 0 && location.getBlockZ() >> 4 == 0
                    : ((Number) args[1]).intValue() == 0 && ((Number) args[2]).intValue() == 0;
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
    public void paperUsesOneNativeSpatialQueryWithoutEnumeratingChunks() {
        owned = proxy(Entity.class, (instance, method, args) -> { throw new AssertionError("Unexpected entity access"); });
        List<Entity> result = List.of(owned);
        origin = proxy(Entity.class, (instance, method, args) -> {
            if (method.getName().equals("getNearbyEntities")) {
                nativeQueries++;
                assertEquals(3.5, args[0]);
                return result;
            }
            throw new AssertionError("Unexpected origin access: " + method.getName());
        });

        assertSame(result, RegionEntityQuery.nearbyOwnedEntities(origin, 3.5));
        assertEquals(1, nativeQueries);
    }

    @Test
    public void foliaSkipsForeignChunksAndEntitiesBeforeReadingTheirData() throws IllegalAccessException {
        foliaField.setBoolean(null, true);
        Entity foreign = proxy(Entity.class, (instance, method, args) -> { throw new AssertionError("Foreign entity read"); });
        owned = proxy(Entity.class, (instance, method, args) -> {
            if (method.getName().equals("getBoundingBox")) {
                return new BoundingBox(8, 64, 8, 9, 65, 9);
            }
            throw new AssertionError("Unexpected owned entity access: " + method.getName());
        });
        Chunk chunk = proxy(Chunk.class, (instance, method, args) -> {
            if (method.getName().equals("getEntities")) {
                chunkQueries++;
                return new Entity[]{origin, owned, foreign};
            }
            throw new AssertionError("Unexpected chunk access: " + method.getName());
        });
        World world = proxy(World.class, (instance, method, args) -> {
            assertEquals("Foreign chunk was inspected", 0, args[0]);
            assertEquals("Foreign chunk was inspected", 0, args[1]);
            return switch (method.getName()) {
                case "isChunkLoaded" -> true;
                case "getChunkAt" -> chunk;
                default -> throw new AssertionError("Unexpected world access: " + method.getName());
            };
        });
        origin = proxy(Entity.class, (instance, method, args) -> switch (method.getName()) {
            case "getLocation" -> new Location(world, 8, 64, 8);
            case "getBoundingBox" -> new BoundingBox(8, 64, 8, 9, 65, 9);
            default -> throw new AssertionError("Unexpected origin access: " + method.getName());
        });

        assertEquals(List.of(owned), RegionEntityQuery.nearbyOwnedEntities(origin, 8));
        assertEquals(1, chunkQueries);
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
