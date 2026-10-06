package com.songoda.core.compatibility;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerVersion {

    private static final Pattern MC_VERSION_IN_PARENS = Pattern.compile("\\(MC: ([0-9]+(?:\\.[0-9]+)*)\\)");

    private static final String SERVER_VERSION;
    private static final KnownServerVersions CURRENT_VERSION;

    static {
        SERVER_VERSION = resolveMinecraftVersion();
        CURRENT_VERSION = init();
    }

    private static String resolveMinecraftVersion() {
        if (Bukkit.getServer() == null) {
            return null;
        }
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion != null) {
            int releaseMarker = bukkitVersion.indexOf("-R");
            if (releaseMarker > 0) {
                return bukkitVersion.substring(0, releaseMarker);
            }
        }
        String serverVersion = Bukkit.getVersion();
        if (serverVersion != null) {
            Matcher matcher = MC_VERSION_IN_PARENS.matcher(serverVersion);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return bukkitVersion;
    }

    private static KnownServerVersions init() {
        return findKnownVersion(SERVER_VERSION);
    }

    private static KnownServerVersions findKnownVersion(String versionString) {
        if (versionString == null) {
            return null;
        }
        for (KnownServerVersions version : KnownServerVersions.values()) {
            if (versionString.equals(version.getVersionString())) {
                return version;
            }
        }
        return null;
    }

    private static String getServerVersion() {
        return SERVER_VERSION != null ? SERVER_VERSION : resolveMinecraftVersion();
    }

    public static boolean isAtLeastVersion(String version) {
        String serverVersion = getServerVersion();
        return serverVersion != null && isVersionAtLeast(serverVersion, version);
    }

    public static boolean isVersionAtLeast(String version, String minimumVersion) {
        return compareVersions(version, minimumVersion) >= 0;
    }

    public static boolean isVersionAtLeast(
            KnownServerVersions version,
            KnownServerVersions minimumVersion) {
        return isVersionAtLeast(version.getVersionString(), minimumVersion.getVersionString());
    }

    public static boolean isAtLeastVersion(@NotNull KnownServerVersions version) {
        return isAtLeastVersion(version.getVersionString());
    }

    private static int compareVersions(String first, String second) {
        if (first == null || second == null || !first.matches("[0-9]+(?:\\.[0-9]+)*")
                || !second.matches("[0-9]+(?:\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid Minecraft version: " + first + " / " + second);
        }
        String[] firstParts = first.split("\\.");
        String[] secondParts = second.split("\\.");
        int length = Math.max(firstParts.length, secondParts.length);
        for (int i = 0; i < length; i++) {
            int firstPart = i < firstParts.length ? Integer.parseInt(firstParts[i]) : 0;
            int secondPart = i < secondParts.length ? Integer.parseInt(secondParts[i]) : 0;
            if (firstPart != secondPart) {
                return Integer.compare(firstPart, secondPart);
            }
        }
        return 0;
    }

    public static boolean isCurrentVersionFullySupported() {
        return getCurrentVersion() != null;
    }

    public static boolean isItemComponentsAvailable() {
        return isAtLeastVersion(KnownServerVersions.V1_21);
    }

    /**
     * {@code minecraft:tooltip_style} data component is available from 1.21.2+.
     */
    public static boolean isTooltipStyleSupported() {
        return isAtLeastVersion(KnownServerVersions.V1_21_2);
    }

    public static KnownServerVersions getCurrentVersion() {
        return CURRENT_VERSION != null ? CURRENT_VERSION : findKnownVersion(getServerVersion());
    }

    public static String getVersionString() {
        return getServerVersion();
    }
}
