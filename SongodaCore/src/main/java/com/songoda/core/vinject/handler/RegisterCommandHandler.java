package com.songoda.core.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import com.songoda.core.utils.CommandUtils;
import com.songoda.core.vinject.annotation.RegisterCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

@Registry(annotation = RegisterCommand.class)
public class RegisterCommandHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if it implements CommandExecutor and/or TabCompleter and register them
        boolean isCommandExecutor = CommandExecutor.class.isAssignableFrom(aClass);
        boolean isTabCompleter = TabCompleter.class.isAssignableFrom(aClass);

        if (!isCommandExecutor) {
            throw new RuntimeException("Class " + aClass.getName() + " annotated with @RegisterCommand must implement CommandExecutor");
        }

        // Get the annotation
        RegisterCommand registerCommand = aClass.getAnnotation(RegisterCommand.class);
        String command = registerCommand.commandName();

        // Create a new instance of the class
        Object instance = component == null ? dependencyContainer.newInstance(aClass) : component;

        // Register the command
        if (isTabCompleter) {
            CommandUtils.registerCommand(command, (CommandExecutor) instance, (TabCompleter) instance, registerCommand.permission());
        } else {
            CommandUtils.registerCommand(command, (CommandExecutor) instance, null, registerCommand.permission());
        }
    }
}
