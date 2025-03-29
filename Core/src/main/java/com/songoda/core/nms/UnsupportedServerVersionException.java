package com.songoda.core.nms;

import com.songoda.core.compatibility.ServerVersion;

public class UnsupportedServerVersionException extends RuntimeException {
    public UnsupportedServerVersionException() {
        this(null);
    }

    public UnsupportedServerVersionException(Throwable cause) {
        this("Your sever version (" + ServerVersion.getServerVersionString() + "; " + ServerVersion.getServerVersion().name() + ") is not fully supported", null);
    }

    protected UnsupportedServerVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
