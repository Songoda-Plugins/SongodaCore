package com.songoda.core.hooks.internal;

import net.vortexdevelopment.vinject.di.ConfigurationContainer;

public final class ConfigReloadHook implements ReloadHook {

    private final Class<?> configClass;

    public ConfigReloadHook(Class<?> configClass) {
        this.configClass = configClass;
    }

    public Class<?> sourceClass() {
        return configClass;
    }

    @Override
    public void onReload() {
        ConfigurationContainer.getInstance().reloadConfig(configClass);
    }
}
