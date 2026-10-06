package com.songoda.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceLogger {

    private static Map<String, Long> timestamps = new ConcurrentHashMap<>();

    public static void start(String key) {
        timestamps.put(key, System.nanoTime());
    }

    public static long stop(String key) {
        Long startTime = timestamps.remove(key);
        if (startTime == null) {
            throw new IllegalArgumentException("No start time recorded for key: " + key);
        }
        return System.nanoTime() - startTime;
    }

    public static void stopAndLog(String key) {
        long duration = stop(key);
        System.out.println("PerformanceLogger - " + key + ": " + duration + " ns (" + (duration / 1_000_000.0) + " ms)");
    }
}
