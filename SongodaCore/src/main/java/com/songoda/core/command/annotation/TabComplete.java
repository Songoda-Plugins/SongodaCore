package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a tab completion provider.
 * <p>
 * Methods annotated with this annotation should return {@link java.util.List}{@code <String>}
 * containing the completion options. The method can use pattern-based or parameter-based completion.
 * <p>
 * For pattern-based completion, use the {@code command} attribute to match a command pattern.
 * For parameter-based completion, use the {@code param} attribute to complete a specific parameter.
 * <p>
 * Examples:
 * <pre>{@code
 * // Pattern-based completion
 * @TabComplete(command = "give {player} {item}")
 * public List<String> completeGiveItem(@Sender CommandSender sender, @Current String current) {
 *     return Arrays.asList("diamond", "iron", "gold");
 * }
 *
 * // Parameter-based completion
 * @TabComplete(param = "player")
 * public List<String> completePlayer(@Sender CommandSender sender, @Current String current) {
 *     return Bukkit.getOnlinePlayers().stream()
 *                  .map(Player::getName)
 *                  .filter(name -> name.startsWith(current))
 *                  .collect(Collectors.toList());
 * }
 * }</pre>
 *
 * @see Current
 * @see TabArgs
 * @see TabIndex
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabComplete {
    /**
     * The command pattern to match for tab completion.
     * Use {@code {param}} for parameters and static text for exact matches.
     * If empty, parameter-based completion will be used instead.
     *
     * @return The command pattern string, or empty string for parameter-based completion
     */
    String command() default "";

    /**
     * The parameter name to complete.
     * If specified, will provide completions for the parameter with this name in any matching command.
     * Takes precedence over {@code command} when both are specified.
     *
     * @return The parameter name, or empty string for pattern-based completion
     */
    String param() default "";

    /**
     * The argument index to complete (0-based).
     * If -1 (default), will complete the last argument.
     * Only used with pattern-based completion ({@code command} attribute).
     *
     * @return The argument index, or -1 for the last argument
     */
    int argIndex() default -1;
}