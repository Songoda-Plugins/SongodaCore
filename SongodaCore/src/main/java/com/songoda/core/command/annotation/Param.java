package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind a method parameter to a named parameter in a command pattern.
 * <p>
 * When used with {@link SubCommand}, this annotation maps the method parameter to a parameter
 * name in the command pattern. If not specified, parameters are resolved by position.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code @Param("player") Player target} - Binds to the "{player}" parameter in the pattern</li>
 *   <li>{@code @Param("**") String allArgs} - Captures all remaining arguments as a single string</li>
 *   <li>{@code @Param String value} - Uses empty string, parameter resolved by position</li>
 * </ul>
 *
 * @see SubCommand
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
    /**
     * The name of the parameter in the command pattern.
     * Use "**" to capture all remaining arguments as a single string.
     * If empty, the parameter will be resolved by position.
     *
     * @return The parameter name, or empty string for positional resolution
     */
    String value() default "";
}