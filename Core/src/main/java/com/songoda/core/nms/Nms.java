package com.songoda.core.nms;

import com.songoda.core.compatibility.ServerVersion;

public final class Nms {
    private static NmsImplementations cachedImplementation;

    /**
     * @return The implementations for the current server version
     */
    public static NmsImplementations getImplementations() throws UnsupportedServerVersionException {
        if (cachedImplementation == null) {
            try {
                Class<?> implementationClazz = Class.forName(getImplementationClassName());
                cachedImplementation = (NmsImplementations) implementationClazz.getConstructors()[0].newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new UnsupportedServerVersionException(ex);
            }
        }

        return cachedImplementation;
    }

    private static String getImplementationClassName() {
        return String.format("com.songoda.core.nms.%s.NmsImplementationsImpl", ServerVersion.getServerVersionString());
    }
}
