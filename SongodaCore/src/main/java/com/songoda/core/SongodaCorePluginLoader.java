package com.songoda.core;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Paper {@link PluginLoader} for the runtime libraries in the filtered
 * {@code META-INF/songodacore-paper-loader-libs.properties} resource.
 * Never probe server classes via {@code Class.forName} here - during bootstrap the plugin loader cannot load {@code io.papermc.paper.Paper}.
 */
public class SongodaCorePluginLoader implements PluginLoader {

    private static final String LIBS_RESOURCE = "/META-INF/songodacore-paper-loader-libs.properties";

    private static Iterable<String> readMavenCoordinates() {
        try (InputStream in = SongodaCorePluginLoader.class.getResourceAsStream(LIBS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + LIBS_RESOURCE);
            }
            Properties props = new Properties();
            props.load(in);
            String line = props.getProperty("maven.coordinates", "").trim();
            if (line.isEmpty()) {
                return java.util.List.of();
            }
            java.util.List<String> coords = new java.util.ArrayList<>();
            for (String part : line.split(",")) {
                String c = part.trim();
                if (!c.isEmpty()) {
                    coords.add(c);
                }
            }
            return coords;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + LIBS_RESOURCE, e);
        }
    }

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        for (String coordinate : readMavenCoordinates()) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinate), null));
        }

        classpathBuilder.addLibrary(resolver);
    }
}
