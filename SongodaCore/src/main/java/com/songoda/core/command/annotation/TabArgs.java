package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject all tab completion arguments into a method parameter.
 * <p>
 * When used in a {@link TabComplete} method, this annotation injects all command arguments
 * as a {@code String[]} array. The parameter type must be {@code String[]}.
 * <p>
 * Example:
 * <pre>{@code
 * @TabComplete(command = "give {player} {item}")
 * public List<String> completeGiveItem(@Sender CommandSender sender,
 *                                      @TabArgs String[] args,
 *                                      @Current String current) {
 *     // args contains all command arguments
 *     // current is the string being completed
 *     return Arrays.asList("diamond", "iron", "gold");
 * }
 * }</pre>
 *
 * @see TabComplete
 * @see Current
 * @see TabIndex
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabArgs {
}