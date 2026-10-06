package com.songoda.core.compatibility;

public enum KnownServerVersions {

    V1_13,
    V1_18,
    V1_18_1,
    V1_18_2,
    V1_19,
    V1_19_1,
    V1_19_2,
    V1_19_3,
    V1_19_4,
    V1_20,
    V1_20_1,
    V1_20_2,
    V1_20_3,
    V1_20_4,
    V1_20_5,
    V1_20_6,
    V1_21,
    V1_21_1,
    V1_21_2,
    V1_21_3,
    V1_21_4,
    V1_21_5,
    V1_21_6,
    V1_21_7,
    V1_21_8,
    V1_21_9,
    V1_21_10,
    V1_21_11,
    V26_1,
    V26_1_2,
    V26_2,
    V26_3;

    private final String versionString;

    KnownServerVersions() {
        this.versionString = this.name().toLowerCase().replace('_', '.').substring(1);
    }

    public String getVersionString() {
        return versionString;
    }
}
