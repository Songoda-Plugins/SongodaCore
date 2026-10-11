package com.songoda.ultimatestacker.api;

/** Test-only reflective API fixture; SongodaCore main sources do not depend on this type. */
public final class UltimateStackerApi {

    private static Object itemManager = new Object();
    private static Object spawnerManager = new Object();
    private static Object blockManager = new Object();

    private UltimateStackerApi() {
    }

    public static Object getItemManager() {
        return itemManager;
    }

    public static Object getSpawnerManager() {
        return spawnerManager;
    }

    public static Object getBlockManager() {
        return blockManager;
    }

    public static void reset() {
        itemManager = new Object();
        spawnerManager = new Object();
        blockManager = new Object();
    }
}
