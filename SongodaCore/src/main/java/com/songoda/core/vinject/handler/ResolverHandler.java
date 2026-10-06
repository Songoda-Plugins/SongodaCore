package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.command.ParameterResolver;
import com.songoda.core.command.annotation.Resolver;
import org.jetbrains.annotations.Nullable;

@Registry(annotation = Resolver.class)
public class ResolverHandler extends AnnotationHandler {
    @Override
    public void handle(Class<?> aClass, @Nullable Object instance, DependencyContainer dependencyContainer) {
        //Check if the class is a parameter resolver
        if (!ParameterResolver.class.isAssignableFrom(aClass)) {
            SongodaPlugin.getInstance().getLogger().warning("Class " + aClass.getName() + " is not a parameter resolver, skipping.");
            return;
        }

        //Register resolver
        SongodaPlugin.getInstance().getCommandManager().registerResolver(instance == null ? (ParameterResolver<?>) dependencyContainer.newInstance(aClass) : (ParameterResolver<?>) instance);
    }
}
