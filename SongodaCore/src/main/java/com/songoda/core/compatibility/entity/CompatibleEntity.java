package com.songoda.core.compatibility.entity;

import lombok.Getter;
import com.songoda.core.compatibility.KnownServerVersions;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.entity.Entity;

public enum CompatibleEntity {

    // 1.19
    ALLAY(KnownServerVersions.V1_19),
    FROG(KnownServerVersions.V1_19),
    TADPOLE(KnownServerVersions.V1_19),
    WARDEN(KnownServerVersions.V1_19),

    // 1.20
    CAMEL(KnownServerVersions.V1_20),
    SNIFFER(KnownServerVersions.V1_20),

    // 1.20.5
    ARMADILLO(KnownServerVersions.V1_20_5),
    BOGGED(KnownServerVersions.V1_20_5),
    BREEZE(KnownServerVersions.V1_20_5),

    // 1.21.4
    CREAKING(KnownServerVersions.V1_21_4);

    @Getter
    private final KnownServerVersions version;

    CompatibleEntity(KnownServerVersions version) {
        this.version = version;
    }

    public boolean isSupported() {
        return ServerVersion.isAtLeastVersion(version);
    }

    public boolean isEntity(Entity entity) {
        return isSupported() && entity.getType().name().equalsIgnoreCase(this.name());
    }

    public void executeIfSupported(Runnable action) {
        if (isSupported()) {
            action.run();
        }
    }

    public void executeIfSupported(Entity entity, Runnable action) {
        if (isSupported() && entity.getType().name().equalsIgnoreCase(this.name())) {
            action.run();
        }
    }
}
