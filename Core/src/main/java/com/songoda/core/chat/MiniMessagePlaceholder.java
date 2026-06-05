package com.songoda.core.chat;

public class MiniMessagePlaceholder {

    public static String PLACEHOLDER_PREFIX = "%";
    public static String PLACEHOLDER_SUFFIX = "%";

    private final String placeholder;
    private final String value;

    public MiniMessagePlaceholder(String placeholder, String value) {
        this.placeholder = PLACEHOLDER_PREFIX + placeholder + PLACEHOLDER_SUFFIX;
        this.value = value;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public String getValue() {
        return this.value;
    }

    public static void setPlaceholderPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be empty");
        }
        if (prefix.equals(PLACEHOLDER_SUFFIX)) {
            throw new IllegalArgumentException("Prefix cannot be the same as the suffix");
        }
        PLACEHOLDER_PREFIX = prefix;
    }

    public static void setPlaceholderSuffix(String suffix) {
        if (suffix == null) {
            throw new IllegalArgumentException("Suffix cannot be null");
        }
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("Suffix cannot be empty");
        }
        if (suffix.equals(PLACEHOLDER_PREFIX)) {
            throw new IllegalArgumentException("Suffix cannot be the same as the prefix");
        }
        PLACEHOLDER_SUFFIX = suffix;
    }

    public static String getPlaceholderPrefix() {
        return PLACEHOLDER_PREFIX;
    }

    public static String getPlaceholderSuffix() {
        return PLACEHOLDER_SUFFIX;
    }
}
