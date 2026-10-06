package com.songoda.core.spi;

/**
 * Holds the active {@link AdventurePlatform}; defaults to reflection-based Paper bridging.
 */
public final class AdventurePlatforms {

    private static volatile AdventurePlatform platform = ReflectionAdventurePlatform.INSTANCE;

    private AdventurePlatforms() {
    }

    public static void install(AdventurePlatform platform) {
        AdventurePlatforms.platform = platform;
    }

    public static AdventurePlatform get() {
        return platform;
    }
}
