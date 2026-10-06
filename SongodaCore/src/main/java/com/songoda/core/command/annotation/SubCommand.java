package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a subcommand handler with pattern matching.
 * <p>
 * The pattern uses placeholders to match command arguments:
 * <ul>
 *   <li>{@code {param}} - Required parameter</li>
 *   <li>{@code {param=default}} - Optional parameter with default value</li>
 *   <li>{@code {**}} - Wildcard parameter capturing all remaining arguments</li>
 * </ul>
 * <p>
 * Parameters can be bound using {@link Param} annotation or resolved by position.
 * <p>
 * Examples:
 * <pre>{@code
 * @SubCommand("give {player} {item} {amount=1}")
 * public void giveItem(@Sender CommandSender sender,
 *                      @Param("player") Player target,
 *                      @Param("item") String item,
 *                      @Param("amount") int amount) {
 *     // Handles: /command give PlayerName diamond 5
 *     // or: /command give PlayerName diamond (amount defaults to 1)
 * }
 *
 * @SubCommand("broadcast {**} {message}")
 * public void broadcast(@Sender CommandSender sender, @Param("message") String message) {
 *     // Handles: /command broadcast This is a message
 * }
 * }</pre>
 *
 * @see Command
 * @see BaseCommand
 * @see Param
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    /**
     * The command pattern to match.
     * Use {@code {param}} for required parameters, {@code {param=default}} for optional parameters
     * with defaults, and {@code {**}} for wildcard parameters.
     *
     * @return The command pattern string
     */
    String value();

    /**
     * Aliases for the command pattern.
     * Each alias follows the same pattern format as the main command.
     *
     * @return Array of alias patterns
     */
    String[] aliases() default {};
}