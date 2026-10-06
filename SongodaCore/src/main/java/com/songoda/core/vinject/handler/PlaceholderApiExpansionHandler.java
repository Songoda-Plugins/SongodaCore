package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.vinject.annotation.PlaceholderApiExpansion;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

@Registry(annotation = PlaceholderApiExpansion.class)
public class PlaceholderApiExpansionHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        try {
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            if (expansionClass.isAssignableFrom(aClass)) {
                Object instance = component != null ? component : dependencyContainer.newInstance(aClass);

                // Invoke register() method on PlaceholderExpansion
                Method registerMethod = expansionClass.getMethod("register");
                registerMethod.invoke(instance);

                // Add to SongodaPlugin's registered expansions to unregister on disable
                SongodaPlugin.getInstance().registerPlaceholderExpansion(instance);
            }
        } catch (Exception e) {
            SongodaPlugin.getInstance().getLogger().warning("Failed to register PlaceholderAPI expansion " + aClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
