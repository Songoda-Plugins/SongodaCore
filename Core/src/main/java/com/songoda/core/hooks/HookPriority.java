package com.songoda.core.hooks;

/**
 * Some handy constants for hook priorities intended to be used in
 * {@link BaseHookRegistry#register(Hook, int)}
 */
public final class HookPriority {
    public static final int HIGHEST = 100;
    public static final int HIGHER = 50;
    public static final int HIGH = 10;
    public static final int NORMAL = 0;
    public static final int LOW = -10;
    public static final int LOWER = -50;

    private HookPriority() {
    }
}
