package com.songoda.core.compatibility;

import lombok.Getter;

public abstract class VersionDependent {

    @Getter
    private final KnownServerVersions version;

    public VersionDependent(KnownServerVersions version) {
        this.version = version;
    }
}
