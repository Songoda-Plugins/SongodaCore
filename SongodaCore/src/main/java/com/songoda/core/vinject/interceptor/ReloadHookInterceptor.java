package com.songoda.core.vinject.interceptor;

import net.vortexdevelopment.vinject.annotation.yaml.YamlConfiguration;
import net.vortexdevelopment.vinject.annotation.yaml.YamlDirectory;
import net.vortexdevelopment.vinject.di.ComponentInterceptor;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.hooks.internal.ConfigReloadHook;
import com.songoda.core.hooks.internal.ReloadHook;
import com.songoda.core.vinject.annotation.RegisterReloadHook;

/**
 * Registers explicit service hooks and automatic reload hooks for YAML configuration beans.
 */
public class ReloadHookInterceptor implements ComponentInterceptor {

    @Override
    public void onComponentRegistered(Class<?> clazz, Object instance, DependencyContainer container) {
        if (clazz.isAnnotationPresent(YamlConfiguration.class) || clazz.isAnnotationPresent(YamlDirectory.class)) {
            SongodaPlugin.getInstance().registerReloadHook(new ConfigReloadHook(clazz));
        }

        if (ReloadHook.class.isAssignableFrom(clazz)) {
            if (clazz.isAnnotationPresent(RegisterReloadHook.class)) {
                SongodaPlugin.getInstance().registerReloadHook((ReloadHook) instance);
            }
        }
    }
}
