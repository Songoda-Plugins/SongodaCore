package com.songoda.core.vinject.serializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class LocationSerializerTest {

    private final LocationSerializer serializer = new LocationSerializer();
    private Field serverField;
    private Object previousServer;
    private World world;

    @Before
    public void installServer() throws ReflectiveOperationException {
        world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (instance, method, arguments) -> switch (method.getName()) {
                    case "getName" -> "world";
                    default -> throw new AssertionError("Unexpected world access: " + method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (instance, method, arguments) -> switch (method.getName()) {
                    case "getWorld" -> "world".equals(arguments[0]) ? world : null;
                    default -> throw new AssertionError("Unexpected server access: " + method.getName());
                });
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        serverField.set(null, server);
    }

    @After
    public void restoreServer() throws IllegalAccessException {
        serverField.set(null, previousServer);
    }

    @Test
    public void deserializesValidPersistedCoordinates() {
        Location location = serializer.deserialize(columns());

        assertNotNull(location);
        assertSame(world, location.getWorld());
        assertEquals(10.5, location.getX(), 0.0);
        assertEquals(64.0, location.getY(), 0.0);
        assertEquals(-3.25, location.getZ(), 0.0);
    }

    @Test
    public void acceptsDifferentJdbcNumericTypes() {
        Map<String, Object> columns = columns();
        columns.put("x", new BigDecimal("10.5"));
        columns.put("y", 64L);
        columns.put("z", BigInteger.valueOf(-3));

        Location location = serializer.deserialize(columns);

        assertNotNull(location);
        assertEquals(10.5, location.getX(), 0.0);
        assertEquals(64.0, location.getY(), 0.0);
        assertEquals(-3.0, location.getZ(), 0.0);
    }

    @Test
    public void missingRequiredValuesReturnNull() {
        assertNull(serializer.deserialize(null));
        for (String column : new String[]{"world", "x", "y", "z"}) {
            Map<String, Object> columns = columns();
            columns.put(column, null);
            assertNull(column, serializer.deserialize(columns));
        }
    }

    @Test
    public void malformedValuesReturnNullWithoutAbortingRowLoading() {
        for (String column : new String[]{"world", "x", "y", "z"}) {
            Map<String, Object> columns = columns();
            columns.put(column, column.equals("world") ? 123 : "invalid coordinate");
            assertNull(column, serializer.deserialize(columns));
        }
        Map<String, Object> columns = columns();
        columns.put("world", " ");
        assertNull(serializer.deserialize(columns));
    }

    @Test
    public void unresolvedWorldReturnsNullAndPreservesPersistedValues() {
        Map<String, Object> columns = columns();
        columns.put("world", "unloaded-world");
        Map<String, Object> original = new HashMap<>(columns);

        assertNull(serializer.deserialize(columns));
        assertEquals(original, columns);
    }

    @Test
    public void nonFiniteCoordinatesReturnNull() {
        for (String column : new String[]{"x", "y", "z"}) {
            for (Number invalid : new Number[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    new BigDecimal("1e1000")}) {
                Map<String, Object> columns = columns();
                columns.put(column, invalid);
                assertNull(column + "=" + invalid, serializer.deserialize(columns));
            }
        }
    }

    @Test
    public void preservesOptionalNumericOrientation() {
        Map<String, Object> columns = columns();
        columns.put("yaw", 90.0);
        columns.put("pitch", -45);

        Location location = serializer.deserialize(columns);

        assertNotNull(location);
        assertEquals(90.0f, location.getYaw(), 0.0f);
        assertEquals(-45.0f, location.getPitch(), 0.0f);
    }

    @Test
    public void malformedOptionalOrientationReturnsNull() {
        for (String column : new String[]{"yaw", "pitch"}) {
            for (Object invalid : new Object[]{"invalid", Float.NaN, Float.POSITIVE_INFINITY, Double.MAX_VALUE}) {
                Map<String, Object> columns = columns();
                columns.put(column, invalid);
                assertNull(column, serializer.deserialize(columns));
            }
        }
    }

    @Test
    public void validLocationSerializationRetainsExistingColumnLayout() {
        Map<String, Object> columns = serializer.serialize(new Location(world, 10.5, 64, -3.25));

        assertEquals(columns(), columns);
        assertNotNull(serializer.deserialize(columns));
    }

    private Map<String, Object> columns() {
        Map<String, Object> columns = new HashMap<>();
        columns.put("world", "world");
        columns.put("x", 10.5);
        columns.put("y", 64.0);
        columns.put("z", -3.25);
        return columns;
    }
}
