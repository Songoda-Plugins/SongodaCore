package com.songoda.core.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationUtilsTest {
    @Test
    void isLocationMatching() {
        assertTrue(LocationUtils.isLocationMatching(
                new Location(null, 10, 20, 30),
                new Location(Mockito.mock(World.class), 10.25, 20.5, 30.75)
        ));
        assertTrue(LocationUtils.isLocationMatching(
                new Location(null, 10, 20, 30),
                new Location(null, 10.25, 20.5, 30.75)
        ));

        assertFalse(LocationUtils.isLocationMatching(
                new Location(null, 10, 20, 30),
                new Location(Mockito.mock(World.class), -10.25, 20.5, 30.75)
        ));
        assertFalse(LocationUtils.isLocationMatching(
                new Location(Mockito.mock(World.class), 10, 20, 30),
                new Location(null, -10.25, 20.5, 30.75)
        ));
    }

    @Disabled("Test not yet implemented")
    @Test
    void isInArea() {
    }
}
