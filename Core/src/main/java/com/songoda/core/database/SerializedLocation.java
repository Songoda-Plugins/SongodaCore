package com.songoda.core.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class SerializedLocation {
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float pitch;
    private final float yaw;

    public SerializedLocation(Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        if (location.getPitch() != 0 && location.getYaw() != 0) {
            this.pitch = location.getPitch();
            this.yaw = location.getYaw();
        } else {
            this.pitch = 0;
            this.yaw = 0;
        }
    }

    public static Location of(Map<String, Object> map) {
        return new Location(Bukkit.getWorld((String) map.get("world")),
                (double) map.get("x"),
                (double) map.get("y"),
                (double) map.get("z"),
                Double.valueOf((double) map.getOrDefault("yaw", 0.0)).floatValue(),
                Double.valueOf((double) map.getOrDefault("pitch", 0.0)).floatValue());
    }

    public static Map<String, Object> of(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        if (location.getPitch() != 0 && location.getYaw() != 0) {
            map.put("pitch", location.getPitch());
            map.put("yaw", location.getYaw());
        }
        return map;
    }

    public Location asLocation() {
        return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z, this.yaw, this.pitch);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", this.world);
        map.put("x", this.x);
        map.put("y", this.y);
        map.put("z", this.z);
        if (this.pitch != 0 && this.yaw != 0) {
            map.put("pitch", this.pitch);
            map.put("yaw", this.yaw);
        }
        return map;
    }
}
