package com.songoda.core.lootables;

import com.songoda.core.lootables.loot.LootManager;

public class Lootables {
    private final String lootablesDir;

    private final LootManager lootManager;

    public Lootables(String lootablesDir) {
        this.lootablesDir = lootablesDir;
        this.lootManager = new LootManager(this);
    }

    public String getLootablesDir() {
        return this.lootablesDir;
    }

    public LootManager getLootManager() {
        return this.lootManager;
    }
}
