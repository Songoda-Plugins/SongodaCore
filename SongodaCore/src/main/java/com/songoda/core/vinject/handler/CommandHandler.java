package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.command.annotation.Command;
import org.jetbrains.annotations.Nullable;

@Registry(annotation = Command.class)
public class CommandHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, @Nullable Object instance, DependencyContainer dependencyContainer) {
        SongodaPlugin.getInstance().getCommandManager().registerCommand(aClass, instance == null ? dependencyContainer.newInstance(aClass) : instance);
    }
}
