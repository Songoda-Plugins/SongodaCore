package com.songoda.core.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pair<K, V> {

    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
