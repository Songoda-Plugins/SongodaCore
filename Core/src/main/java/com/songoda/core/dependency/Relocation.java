package com.songoda.core.dependency;

public class Relocation {
    private final String from;
    private final String to;

    public Relocation(String from, String to) {
        if (from == null) {
            throw new IllegalArgumentException("'from' cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("'to' cannot be null");
        }

        this.from = from.replaceAll(";", ".");
        this.to = to.replaceAll(";", ".");
    }

    public String getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }
}
