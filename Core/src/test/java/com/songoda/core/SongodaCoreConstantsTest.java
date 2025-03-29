package com.songoda.core;

import org.junit.jupiter.api.Test;
import org.opentest4j.TestSkippedException;

import java.util.Objects;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SongodaCoreConstantsTest {
    // Pattern is from https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    @Test
    void getCoreVersion() {
        if (!Objects.equals(System.getenv("TESTS_RUN_WITH_MAVEN"), "true")) {
            throw new TestSkippedException("Skipping test because it requires the TESTS_RUN_WITH_MAVEN environment variable to be set to true");
        }

        String coreVersion = SongodaCoreConstants.getCoreVersion();

        assertTrue(VERSION_PATTERN.matcher(coreVersion).matches(), "Version string is not a valid semver string: " + coreVersion);
    }

    @Test
    void getProjectName() {
        assertFalse(SongodaCoreConstants.getProjectName().isEmpty());
    }

    @Test
    void getGitHubProjectUrl() {
        assertFalse(SongodaCoreConstants.getGitHubProjectUrl().isEmpty());
    }
}
