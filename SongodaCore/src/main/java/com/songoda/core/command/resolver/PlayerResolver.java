package com.songoda.core.command.resolver;

import com.songoda.core.command.ParameterResolver;
import com.songoda.core.command.annotation.Resolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

@Resolver
public class PlayerResolver implements ParameterResolver<Player> {
    @Override
    public Player resolve(String input) {
        return Bukkit.getPlayer(input);
    }

    @Override
    public boolean supports(Class<?> type) {
        return Player.class.isAssignableFrom(type);
    }

    @Override
    public Set<Class<?>> getSupportedTypes() {
        return Set.of(Player.class);
    }
}