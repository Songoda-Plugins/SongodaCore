package com.songoda.core.database;

public class OptionalResult {
    private final Object value;
    private final boolean present;

    public OptionalResult(Object value, boolean present) {
        this.value = value;
        this.present = present;
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(this.value);
    }

    public boolean isPresent() {
        return this.present;
    }

    public <V> V getOrDefault(V defaultValue) {
        return this.present ? (V) this.value : defaultValue;
    }

    public static OptionalResult empty() {
        return new OptionalResult(null, false);
    }

    public static <T> OptionalResult of(T value) {
        return new OptionalResult(value, true);
    }
}
