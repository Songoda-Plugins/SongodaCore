package com.songoda.core;

/**
 * Some return values in this class are automatically replaced by a maven plugin after the project has been compiled.
 * This allows for properties to be defined at one place without relying on a text file
 * that needs to be inside the final jar (might get lost when this lib is shaded into other projects).
 * <p>
 * <b>!! Manually changing the values in this class has to be considered a breaking change. !!</b>
 */
public class CraftaroCoreConstants {
    private CraftaroCoreConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static String getCoreVersion() {
        return "UNKNOWN_VESION";
    }

    public static String getProjectName() {
        return "CraftaroCore";
    }

    public static String getGitHubProjectUrl() {
        return "https://github.com/craftaro/SongodaCore";
    }
}
