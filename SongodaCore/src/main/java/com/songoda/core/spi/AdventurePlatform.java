package com.songoda.core.spi;

import net.kyori.adventure.text.Component;

/**
 * Bridges server-loaded Adventure {@code Component} types and plugin-visible {@link Component} where class loaders differ.
 */
public interface AdventurePlatform {

    Class<?> getServerComponentClass();

    Component convertToShadedComponent(Object original);

    Object convertToOriginalComponent(String json);

    Object convertToOriginalComponent(Component component);
}
