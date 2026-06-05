package com.songoda.core.nms.world;

import java.lang.reflect.InvocationTargetException;

public interface BBaseSpawner {
    boolean isNearPlayer() throws InvocationTargetException, IllegalAccessException;

    /**
     * <b>Ignores {@link #isNearPlayer()} - Make sure the server isn't already ticking the spawner!</b>
     */
    void tick() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException;
}
