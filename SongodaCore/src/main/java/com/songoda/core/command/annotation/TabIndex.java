package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the current tab completion argument index into a method parameter.
 * <p>
 * When used in a {@link TabComplete} method, this annotation injects the zero-based index
 * of the argument currently being completed. The parameter type must be {@code int} or {@link Integer}.
 * <p>
 * Example:
 * <pre>{@code
 * @TabComplete(command = "give {player} {item}")
 * public List<String> completeGiveItem(@Sender CommandSender sender,
 *                                      @TabIndex int index,
 *                                      @Current String current) {
 *     // index is 0 for {player}, 1 for {item}
 *     // current is the string being completed
 *     if (index == 0) {
 *         return getPlayerNames(current);
 *     } else {
 *         return getItemNames(current);
 *     }
 * }
 * }</pre>
 *
 * @see TabComplete
 * @see Current
 * @see TabArgs
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabIndex {
}