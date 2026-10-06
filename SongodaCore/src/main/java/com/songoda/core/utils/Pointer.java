package com.songoda.core.utils;

public class Pointer<T> {

    private T value;

    public Pointer(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
