package com.songoda.core.vinject.serializer;

import net.vortexdevelopment.vinject.annotation.database.Column;
import net.vortexdevelopment.vinject.annotation.database.MethodValue;
import net.vortexdevelopment.vinject.annotation.database.RegisterDatabaseSerializer;
import net.vortexdevelopment.vinject.database.serializer.DatabaseSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

@RegisterDatabaseSerializer(Location.class)
public class LocationSerializer extends DatabaseSerializer<Location> {

    @MethodValue("getWorld.getName")  // Extract: location.getWorld().getName()
    @Column(name = "world", length = 255)
    private String world;

    @MethodValue("getX")  // Extract: location.getX()
    @Column(name = "x")
    private Double x;

    @MethodValue("getY")  // Extract: location.getY()
    @Column(name = "y")
    private Double y;

    @MethodValue("getZ")  // Extract: location.getZ()
    @Column(name = "z")
    private Double z;

//    @MethodValue("getYaw")  // Extract: location.getYaw()
//    @Column(name = "yaw")
//    private Float yaw;
//
//    @MethodValue("getPitch")  // Extract: location.getPitch()
//    @Column(name = "pitch")
//    private Float pitch;

    // Optional: Override deserialize if you need custom logic (e.g., World lookup)
    @Override
    public Location deserialize(Map<String, Object> columnValues) {
        String worldName = (String) columnValues.get("world");
        Double x = (Double) columnValues.get("x");
        Double y = (Double) columnValues.get("y");
        Double z = (Double) columnValues.get("z");
        Float yaw = (Float) columnValues.get("yaw");
        Float pitch = (Float) columnValues.get("pitch");

        if (worldName == null || x == null || y == null || z == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        Location location = new Location(world, x, y, z);
        if (yaw != null) location.setYaw(yaw);
        if (pitch != null) location.setPitch(pitch);
        return location;
    }
}