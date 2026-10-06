package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as the base/default command handler.
 * <p>
 * This method will be executed when the command is invoked without any subcommand arguments.
 * The method should be in a class annotated with {@link Command}.
 * <p>
 * Example:
 * <pre>{@code
 * @Command("mycommand")
 * public class MyCommand {
 *     @BaseCommand
 *     public void onBaseCommand(@Sender CommandSender sender) {
 *         sender.sendMessage("This is the base command!");
 *     }
 * }
 * }</pre>
 *
 * @see Command
 * @see SubCommand
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BaseCommand {
    /**
     * Optional description of the base command.
     * Currently used for documentation purposes.
     *
     * @return The command description
     */
    String description() default "";
}