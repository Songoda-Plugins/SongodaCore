package com.songoda.core.command;

import java.util.Set;

/**
 * Interface for resolving string command arguments to specific types.
 * <p>
 * Implementations of this interface can be registered with the {@link com.songoda.core.command.CommandManager}
 * to automatically convert string arguments to custom types during command execution.
 * <p>
 * Resolvers should be annotated with {@link com.songoda.core.command.annotation.Resolver}
 * to be automatically discovered and registered.
 *
 * @param <T> The type that this resolver converts strings to
 * @see com.songoda.core.command.annotation.Resolver
 */
public interface ParameterResolver<T> {
    /**
     * Resolve the input string to the target type
     *
     * @param input The input string to resolve
     * @return The resolved object, or null if resolution failed
     */
    T resolve(String input);

    /**
     * Check if this resolver supports the given type
     *
     * @param type The type to check
     * @return True if this resolver supports the type
     */
    boolean supports(Class<?> type);

    /**
     * Get all types supported by this resolver
     *
     * @return A set of supported types
     */
    Set<Class<?>> getSupportedTypes();
}