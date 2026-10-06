package com.songoda.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the command sender into a method parameter.
 * <p>
 * This annotation automatically injects the {@link org.bukkit.command.CommandSender} who executed
 * the command. The parameter type can be {@link org.bukkit.command.CommandSender}, {@link org.bukkit.entity.Player},
 * or any subclass that is compatible with the actual sender type.
 * <p>
 * Example:
 * <pre>{@code
 * @SubCommand("give {player} {item}")
 * public void giveItem(@Sender CommandSender sender, @Param("player") Player target, @Param("item") String item) {
 *     // sender is automatically injected
 * }
 * }</pre>
 *
 * @see org.bukkit.command.CommandSender
 * @see org.bukkit.entity.Player
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sender {
}