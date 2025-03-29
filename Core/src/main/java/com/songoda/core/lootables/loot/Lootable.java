package com.songoda.core.lootables.loot;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lootable {
    // The key applicable to this lootable.
    @SerializedName("Type")
    private final String type;

    // Registered loot.
    @SerializedName("Loot")
    private final List<Loot> registeredLoot = new ArrayList<>();

    public Lootable(String key) {
        this.type = key;
    }

    public Lootable(String key, Loot... loots) {
        this.type = key;

        this.registeredLoot.addAll(Arrays.asList(loots));
    }

    public List<Loot> getRegisteredLoot() {
        return new ArrayList<>(this.registeredLoot);
    }

    public void registerLoot(Loot... loots) {
        this.registeredLoot.addAll(Arrays.asList(loots));
    }

    public String getKey() {
        return this.type;
    }

    public void removeLoot(Loot loot) {
        this.registeredLoot.remove(loot);
    }
}
