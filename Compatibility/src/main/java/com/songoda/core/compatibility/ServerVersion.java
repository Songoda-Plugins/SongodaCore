package com.songoda.core.compatibility;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public enum ServerVersion {

    UNKNOWN,

    // 1.7.x
    V1_7, V1_7_2, V1_7_4, V1_7_5, V1_7_6, V1_7_7, V1_7_8, V1_7_9, V1_7_10,

    // 1.8.x
    V1_8, V1_8_1, V1_8_2, V1_8_3, V1_8_4, V1_8_5, V1_8_6, V1_8_7, V1_8_8, V1_8_9,

    // 1.9.x
    V1_9, V1_9_1, V1_9_2, V1_9_3, V1_9_4,

    // 1.10.x
    V1_10, V1_10_1, V1_10_2,

    // 1.11.x
    V1_11, V1_11_1, V1_11_2,

    // 1.12.x
    V1_12, V1_12_1, V1_12_2, V1_13, V1_13_1, V1_13_2,

    // 1.14.x
    V1_14, V1_14_1, V1_14_2, V1_14_3, V1_14_4,

    // 1.15.x
    V1_15, V1_15_1, V1_15_2,

    // 1.16.x
    V1_16, V1_16_1, V1_16_2, V1_16_3, V1_16_4, V1_16_5,

    // 1.17.x
    V1_17, V1_17_1,

    // 1.18.x
    V1_18, V1_18_1, V1_18_2,

    // 1.19.x
    V1_19, V1_19_1, V1_19_2, V1_19_3, V1_19_4,

    // 1.20.x
    V1_20, V1_20_1, V1_20_2, V1_20_3, V1_20_4, V1_20_5, V1_20_6,

    // 1.21.x
    V1_21, V1_21_1, V1_21_2, V1_21_3, V1_21_4, V1_21_5, V1_21_6, V1_21_7, V1_21_8, V1_21_9, V1_21_10,

    // 1.22.x
    V1_22,

    // 1.23.x
    V1_23,

    ;

    static final String serverPackageVersion;
    private static final String serverReleaseVersion;
    private static final ServerVersion serverVersion;
    private static final boolean isMocked;
    private static final String minecraftVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];

    private static final Map<String, String> VERSION_TO_REVISION;

    static {
        VERSION_TO_REVISION = new HashMap<>();
        VERSION_TO_REVISION.put("1.20", "v1_20_R1");
        VERSION_TO_REVISION.put("1.20.1", "v1_20_R1");
        VERSION_TO_REVISION.put("1.20.2", "v1_20_R2");
        VERSION_TO_REVISION.put("1.20.3", "v1_20_R3");
        VERSION_TO_REVISION.put("1.20.4", "v1_20_R3");
        VERSION_TO_REVISION.put("1.20.5", "v1_20_R4");
        VERSION_TO_REVISION.put("1.20.6", "v1_20_R4");
        VERSION_TO_REVISION.put("1.21", "v1_21_R1");
        VERSION_TO_REVISION.put("1.21.1", "v1_21_R1");
        VERSION_TO_REVISION.put("1.21.3", "v1_21_R2");
        VERSION_TO_REVISION.put("1.21.4", "v1_21_R3");
        VERSION_TO_REVISION.put("1.21.5", "v1_21_R4");
        VERSION_TO_REVISION.put("1.21.6", "v1_21_R5");
        VERSION_TO_REVISION.put("1.21.7", "v1_21_R5");
        VERSION_TO_REVISION.put("1.21.8", "v1_21_R5");
        VERSION_TO_REVISION.put("1.21.9", "v1_21_R6");
        VERSION_TO_REVISION.put("1.21.10", "v1_21_R6");

        if (Bukkit.getServer() != null) {
            String srvPackage = Bukkit.getServer().getClass().getPackage().getName();
            isMocked = srvPackage.equals("be.seeseemelk.mockbukkit");

            if (isMocked) {
                serverPackageVersion = "v" + Bukkit.getServer().getBukkitVersion().replace('.', '_') + "_R0";
                serverReleaseVersion = "0";
            } else {
                String nmsVersion;
                try {
                    nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                } catch (Exception ex) {
                    nmsVersion = VERSION_TO_REVISION.getOrDefault(minecraftVersion, "");

                    if (nmsVersion.isEmpty()) {
                        new RuntimeException("Cannot detect NMS version for server version: " + minecraftVersion).printStackTrace();
                    }
                }

                serverPackageVersion = nmsVersion;
                serverReleaseVersion = serverPackageVersion.indexOf('R') != -1 ? serverPackageVersion.substring(serverPackageVersion.indexOf('R') + 1) : "";
            }
        } else {
            serverPackageVersion = "Bukkit-not-initialized";
            serverReleaseVersion = "Bukkit-not-initialized";
            isMocked = false;
        }

        serverVersion = getVersion();
    }

    public static String getMinecraftVersion() {
        return minecraftVersion;
    }

    private static ServerVersion getVersion() {
        String version = "V" + minecraftVersion.replace(".", "_");
        try {
            return ServerVersion.valueOf(version);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    public boolean isLessThan(ServerVersion other) {
        if (serverVersion == UNKNOWN) {
            return false;
        }

        return this.ordinal() < other.ordinal();
    }

    public boolean isAtOrBelow(ServerVersion other) {
        if (serverVersion == UNKNOWN && other != UNKNOWN) {
            return false;
        }

        return this.ordinal() <= other.ordinal();
    }

    public boolean isGreaterThan(ServerVersion other) {
        if (serverVersion == UNKNOWN) {
            return false;
        }

        return this.ordinal() > other.ordinal();
    }

    public boolean isAtLeast(ServerVersion other) {
        if (serverVersion == UNKNOWN && other != UNKNOWN) {
            return false;
        }

        return this.ordinal() >= other.ordinal();
    }

    public static String getServerVersionString() {
        return serverPackageVersion;
    }

    public static String getVersionReleaseNumber() {
        return serverReleaseVersion;
    }

    public static ServerVersion getServerVersion() {
        return serverVersion;
    }

    public static boolean isServerVersion(ServerVersion version) {
        return serverVersion == version;
    }

    public static boolean isServerVersion(ServerVersion... versions) {
        return ArrayUtils.contains(versions, serverVersion);
    }

    public static boolean isServerVersionAbove(ServerVersion version) {
        if (serverVersion == UNKNOWN) {
            return false;
        }

        return serverVersion.ordinal() > version.ordinal();
    }

    public static boolean isServerVersionAtLeast(ServerVersion version) {
        if (serverVersion == UNKNOWN && version != UNKNOWN) {
            return false;
        }

        return serverVersion.ordinal() >= version.ordinal();
    }

    public static boolean isServerVersionAtOrBelow(ServerVersion version) {
        if (serverVersion == UNKNOWN && version != UNKNOWN) {
            return false;
        }

        return serverVersion.ordinal() <= version.ordinal();
    }

    public static boolean isServerVersionBelow(ServerVersion version) {
        if (serverVersion == UNKNOWN) {
            return false;
        }

        return serverVersion.ordinal() < version.ordinal();
    }
}
