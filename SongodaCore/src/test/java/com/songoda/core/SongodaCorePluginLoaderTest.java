package com.songoda.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SongodaCorePluginLoaderTest {

    @Test
    public void paperLoaderUsesTheSameResolvedLibrariesAsTheBukkitDescriptor() throws Exception {
        Method readCoordinates = SongodaCorePluginLoader.class.getDeclaredMethod("readMavenCoordinates");
        readCoordinates.setAccessible(true);
        Iterable<?> coordinates = (Iterable<?>) readCoordinates.invoke(null);
        List<String> loaderLibraries = new ArrayList<>();
        for (Object coordinate : coordinates) {
            String library = (String) coordinate;
            assertFalse("Maven must resolve every library version", library.contains("${"));
            loaderLibraries.add(library);
        }
        assertFalse("The Paper loader must have runtime libraries", loaderLibraries.isEmpty());

        try (InputStream descriptor = SongodaCorePluginLoader.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull("The Bukkit library descriptor must be bundled", descriptor);
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(descriptor, StandardCharsets.UTF_8));
            assertEquals(configuration.getStringList("libraries"), loaderLibraries);
        }
    }
}
