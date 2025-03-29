package com.songoda.core.dependency;

import com.songoda.core.SongodaCoreConstants;
import com.songoda.core.SongodaCore;
import com.georgev22.api.libraryloader.ClassLoaderAccess;
import com.georgev22.api.libraryloader.LibraryLoader;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DependencyLoader {
    private static final int DEPENDENCY_VERSION = 1;

    private final LibraryLoader libraryLoader;
    private final ClassLoaderAccess parentClassLoaderAccess;

    public DependencyLoader(Plugin plugin) {
        // Bind loaded dependencies to the plugin's parent class loader so classes could be accessed across plugins
        ClassLoader parentClassLoader = plugin.getClass().getClassLoader().getParent();
        if (parentClassLoader instanceof URLClassLoader) {
            this.libraryLoader = new LibraryLoader(
                    (URLClassLoader) parentClassLoader,
                    new File(plugin.getDataFolder().getParentFile(), SongodaCoreConstants.getProjectName() + "/dependencies/v" + DEPENDENCY_VERSION),
                    SongodaCore.getLogger()
            );
            this.parentClassLoaderAccess = new ClassLoaderAccess((URLClassLoader) parentClassLoader);
        } else {
            // We have AppClassLoader here
            this.libraryLoader = new LibraryLoader(
                    parentClassLoader,
                    new File(plugin.getDataFolder().getParentFile(), SongodaCoreConstants.getProjectName() + "/dependencies/v" + DEPENDENCY_VERSION),
                    SongodaCore.getLogger()
            );
            this.parentClassLoaderAccess = new ClassLoaderAccess(parentClassLoader);
        }
    }

    public void loadDependencies(Collection<Dependency> dependencies) throws IOException {
        for (Dependency dependency : dependencies) {
            loadDependency(dependency);
        }
    }

    public void loadDependency(Dependency dependency) throws IOException {
        String name = dependency.getArtifactId() + "-" + dependency.getVersion();
        File outputFile = new File(this.libraryLoader.getLibFolder(), dependency.getGroupId().replace(".", File.separator) + File.separator + dependency.getArtifactId().replace(".", File.separator) + File.separator + dependency.getVersion() + File.separator + "raw-" + name + ".jar");
        File relocatedFile = new File(outputFile.getParentFile(), name.replace("raw-", "") + ".jar");
        if (relocatedFile.exists()) {
            if (isJarLoaded(relocatedFile)) {
                return;
            }

            loadJarIntoClasspath(relocatedFile, dependency);
            return;
        }

        SongodaCore.getLogger().info(String.format("Downloading dependency %s:%s:%s from %s", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getRepositoryUrl()));
        Files.createDirectories(outputFile.getParentFile().toPath());
        try (InputStream is = new URL(dependency.buildArtifactUrl()).openStream()) {
            Files.copy(is, outputFile.toPath());
        }

        loadJarIntoClasspath(outputFile, dependency);
    }

    public void loadJarIntoClasspath(File file, Dependency dependency) throws IOException {
        if (!isRelocated(file) && dependency.shouldRelocate()) {
            SongodaCore.getLogger().info("Loading dependency for relocation " + file);
            // relocate package to com.songoda.core.third_party to avoid conflicts
            List<Relocation> relocations = new ArrayList<>();

            for (com.songoda.core.dependency.Relocation r : dependency.getRelocations()) {
                relocations.add(new Relocation(r.getFrom(), r.getTo()));
            }

            // Relocate the classes
            File finalJar = new File(file.getParentFile(), file.getName().replace("raw-", ""));
            JarRelocator relocator = new JarRelocator(file, finalJar, relocations);
            try {
                relocator.run();
                SongodaCore.getLogger().info("Relocated dependency " + file);

                // Delete the old jar
                Files.deleteIfExists(file.toPath());
            } catch (Exception e) {
                SongodaCore.getLogger().severe("Failed to relocate dependency " + file);
                if (e.getMessage().contains("zip file is empty")) {
                    SongodaCore.getLogger().severe("Try deleting '" + this.libraryLoader.getLibFolder().getParent() + "' and restarting the server");
                }

                // Delete the new jar cuz it's probably corrupted
                Files.deleteIfExists(finalJar.toPath());
                throw e;
            }
        }

        try {
            // Do not check the path here, it uses the original non-relocated paths. Use isJarLoaded instead
            this.libraryLoader.load(new LibraryLoader.Dependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getRepositoryUrl()), false);
        } catch (Exception ex) {
            // Something went wrong
            ex.printStackTrace();
        }
        SongodaCore.getLogger().info("----------------------------");
    }

    private boolean isRelocated(File jarFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                if (entry.getName().startsWith("com/songoda/third_party")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Finds the first .class file in the jar and check if it's loaded
     */
    private boolean isJarLoaded(File jarFile) throws IOException {
        URL jarFileURL = jarFile.toURI().toURL();
        try {
            return this.parentClassLoaderAccess.getPathURLs().stream().anyMatch(url -> url.getFile().equals(jarFileURL.getFile()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getDependencyVersion() {
        return DEPENDENCY_VERSION;
    }
}
