package com.songoda.core.vinject.serializer;

import net.vortexdevelopment.vinject.annotation.database.Column;
import net.vortexdevelopment.vinject.annotation.database.MethodValue;
import net.vortexdevelopment.vinject.annotation.database.RegisterDatabaseSerializer;
import net.vortexdevelopment.vinject.database.serializer.DatabaseSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public @Nullable Location deserialize(@Nullable Map<String, Object> columnValues) {
        if (columnValues == null
                || !(columnValues.get("world") instanceof String worldName) || worldName.isBlank()
                || !(columnValues.get("x") instanceof Number x)
                || !(columnValues.get("y") instanceof Number y)
                || !(columnValues.get("z") instanceof Number z)) {
            return null;
        }

        double locationX = x.doubleValue();
        double locationY = y.doubleValue();
        double locationZ = z.doubleValue();
        if (!Double.isFinite(locationX) || !Double.isFinite(locationY) || !Double.isFinite(locationZ)) {
            return null;
        }

        Object yawValue = columnValues.get("yaw");
        Object pitchValue = columnValues.get("pitch");
        if ((yawValue != null && (!(yawValue instanceof Number yaw) || !Float.isFinite(yaw.floatValue())))
                || (pitchValue != null && (!(pitchValue instanceof Number pitch) || !Float.isFinite(pitch.floatValue())))) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        Location location = new Location(world, locationX, locationY, locationZ);
        if (yawValue instanceof Number yaw) {
            location.setYaw(yaw.floatValue());
        }
        if (pitchValue instanceof Number pitch) {
            location.setPitch(pitch.floatValue());
        }
        return location;
    }
}
