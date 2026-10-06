package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a parameter resolver for automatic registration.
 * <p>
 * Classes annotated with {@code @Resolver} that implement {@link com.songoda.core.command.ParameterResolver}
 * will be automatically discovered and registered with the {@link com.songoda.core.command.CommandManager}
 * during plugin initialization.
 * <p>
 * Example:
 * <pre>{@code
 * @Resolver
 * public class PlayerResolver implements ParameterResolver<Player> {
 *     @Override
 *     public Player resolve(String input) {
 *         return Bukkit.getPlayer(input);
 *     }
 *
 *     @Override
 *     public boolean supports(Class<?> type) {
 *         return Player.class.isAssignableFrom(type);
 *     }
 *
 *     @Override
 *     public Set<Class<?>> getSupportedTypes() {
 *         return Set.of(Player.class);
 *     }
 * }
 * }</pre>
 *
 * @see com.songoda.core.command.ParameterResolver
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolver {
}
