package com.songoda.core.utils;

import org.bukkit.Location;

public class LocationUtils {
    /**
     * Compares the block coordinates of two locations <strong>ignoring the world</strong>
     */
    public static boolean isLocationMatching(Location location1, Location location2) {
        return location1.getBlockX() == location2.getBlockX() &&
                location1.getBlockY() == location2.getBlockY() &&
                location1.getBlockZ() == location2.getBlockZ();
    }

    public static boolean isInArea(Location location, Location pos1, Location pos2) {
        double x1 = Math.min(pos1.getX(), pos2.getX());
        double y1 = Math.min(pos1.getY(), pos2.getY());
        double z1 = Math.min(pos1.getZ(), pos2.getZ());

        double x2 = Math.max(pos1.getX(), pos2.getX());
        double y2 = Math.max(pos1.getY(), pos2.getY());
        double z2 = Math.max(pos1.getZ(), pos2.getZ());

        return location.getX() >= x1 && location.getX() <= x2 &&
                location.getY() >= y1 && location.getY() <= y2 &&
                location.getZ() >= z1 && location.getZ() <= z2;
    }

    public static Location getCenter(Location location) {
        double xOffset = location.getBlockX() > 0 ? 0.5 : -0.5;
        double zOffset = location.getBlockZ() > 0 ? 0.5 : -0.5;
        return new Location(
                location.getWorld(),
                location.getBlockX() + xOffset,
                location.getBlockY(),
                location.getBlockZ() + zOffset,
                0,
                0
        );
    }
}
