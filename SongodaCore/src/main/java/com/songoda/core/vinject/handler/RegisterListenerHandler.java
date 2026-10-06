package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.vinject.annotation.RegisterListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

@Registry(annotation = RegisterListener.class)
public class RegisterListenerHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if the class is a org.bukkit.event.Listener
        if (Listener.class.isAssignableFrom(aClass)) {
            Object instance = component != null ? component : dependencyContainer.newInstance(aClass);

            //Register the listener
            Bukkit.getPluginManager().registerEvents((Listener) instance, SongodaPlugin.getInstance());
        }
    }
}
