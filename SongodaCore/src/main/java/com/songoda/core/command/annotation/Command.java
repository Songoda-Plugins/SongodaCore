package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a command handler.
 * <p>
 * Classes annotated with {@code @Command} will be automatically registered as Bukkit commands.
 * Methods within the class can be annotated with {@link BaseCommand} for the default handler
 * and {@link SubCommand} for subcommand handlers.
 * <p>
 * Example:
 * <pre>{@code
 * @Command("mycommand")
 * public class MyCommand {
 *     @BaseCommand
 *     public void onBaseCommand(@Sender CommandSender sender) {
 *         sender.sendMessage("Base command!");
 *     }
 *
 *     @SubCommand("sub {param}")
 *     public void onSubCommand(@Sender CommandSender sender, @Param("param") String param) {
 *         sender.sendMessage("Subcommand with param: " + param);
 *     }
 * }
 * }</pre>
 *
 * @see BaseCommand
 * @see SubCommand
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * The name of the command.
     * This will be the primary command name registered with Bukkit.
     *
     * @return The command name
     */
    String value();

    /**
     * Aliases for the command.
     * These will be registered as alternative command names.
     *
     * @return Array of command aliases
     */
    String[] aliases() default {};
}