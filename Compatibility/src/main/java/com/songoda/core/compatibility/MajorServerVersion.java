package com.songoda.core.compatibility;

import org.apache.commons.lang3.ArrayUtils;

public enum MajorServerVersion {
    UNKNOWN, V1_7, V1_8, V1_9, V1_10, V1_11, V1_12, V1_13, V1_14, V1_15, V1_16, V1_17, V1_18, V1_19, V1_20, V1_21, V1_22,
    V26_1;

    private static final MajorServerVersion SERVER_VERSION = getVersion();

    public boolean isLessThan(MajorServerVersion other) {
        if (SERVER_VERSION == UNKNOWN) {
            return false;
        }

        return this.ordinal() < other.ordinal();
    }

    public boolean isAtOrBelow(MajorServerVersion other) {
        if (SERVER_VERSION == UNKNOWN && other != UNKNOWN) {
            return false;
        }

        return this.ordinal() <= other.ordinal();
    }

    public boolean isGreaterThan(MajorServerVersion other) {
        if (SERVER_VERSION == UNKNOWN) {
            return false;
        }

        return this.ordinal() > other.ordinal();
    }

    public boolean isAtLeast(MajorServerVersion other) {
        if (SERVER_VERSION == UNKNOWN && other != UNKNOWN) {
            return false;
        }

        return this.ordinal() >= other.ordinal();
    }

    public static MajorServerVersion getServerVersion() {
        return SERVER_VERSION;
    }

    public static boolean isServerVersion(MajorServerVersion version) {
        return SERVER_VERSION == version;
    }

    public static boolean isServerVersion(MajorServerVersion... versions) {
        return ArrayUtils.contains(versions, SERVER_VERSION);
    }

    public static boolean isServerVersionAbove(MajorServerVersion version) {
        if (SERVER_VERSION == UNKNOWN) {
            return false;
        }

        return SERVER_VERSION.ordinal() > version.ordinal();
    }

    public static boolean isServerVersionAtLeast(MajorServerVersion version) {
        if (SERVER_VERSION == UNKNOWN && version != UNKNOWN) {
            return false;
        }

        return SERVER_VERSION.ordinal() >= version.ordinal();
    }

    public static boolean isServerVersionAtOrBelow(MajorServerVersion version) {
        if (SERVER_VERSION == UNKNOWN && version != UNKNOWN) {
            return false;
        }

        return SERVER_VERSION.ordinal() <= version.ordinal();
    }

    public static boolean isServerVersionBelow(MajorServerVersion version) {
        if (SERVER_VERSION == UNKNOWN) {
            return false;
        }

        return SERVER_VERSION.ordinal() < version.ordinal();
    }

    private static MajorServerVersion getVersion() {
        for (MajorServerVersion version : values()) {
            if (ServerVersion.serverPackageVersion.toUpperCase().startsWith(version.name())) {
                return version;
            }
        }

        return UNKNOWN;
    }
}
