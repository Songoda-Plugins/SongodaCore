package com.songoda.core.utils;

import com.songoda.core.SongodaCore;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @deprecated The class is not used in the Core and existing usages in plugins should be re-implemented in the Core for
 *         better stability between game versions.
 */
@Deprecated
public class ReflectionUtils {
    public final static double JAVA_VERSION = getVersion();
    private static String system_os = System.getProperty("os.name").toLowerCase();

    private static double getVersion() {
        String version = System.getProperty("java.version");
        int i = version.indexOf('.');

        if (i != -1 && (i = version.indexOf('.', i + 1)) != -1) {
            return Double.parseDouble(version.substring(0, i));
        }

        return Double.NaN;
    }

    public static File getJarFile(Class jarClass) {
        return new File(jarClass.getProtectionDomain().getCodeSource().getLocation().getPath().
                replace("%20", " ").replace("%25", "%"));
    }

    public static void setPrivateField(Class<?> c, Object handle, String fieldName, Object value) throws Exception {
        Field f = c.getDeclaredField(fieldName);
        f.setAccessible(true);

        f.set(handle, value);
    }

    public static Object getPrivateField(Class<?> c, Object handle, String fieldName) throws Exception {
        Field field = c.getDeclaredField(fieldName);
        field.setAccessible(true);

        return field.get(handle);
    }

    public static Object invokePrivateMethod(Class<?> c, String methodName, Object handle, Class[] types, Object[] parameters) throws Exception {
        Method m = c.getDeclaredMethod(methodName, types);
        m.setAccessible(true);

        return m.invoke(handle, parameters);
    }

    // does not work in JRE 8+
    private static Method j7getStackTraceElementMethod;
    private static Method j7getStackTraceDepthMethod;

    // does not work in JRE != 8
    private static Method j8getJavaLangAccess;
    private static Method j8getStackTraceElementMethod;

    static {
        try {
            j7getStackTraceElementMethod = Throwable.class.getDeclaredMethod("getStackTraceElement", int.class);
            j7getStackTraceElementMethod.setAccessible(true);
            j7getStackTraceDepthMethod = Throwable.class.getDeclaredMethod("getStackTraceDepth");
            j7getStackTraceDepthMethod.setAccessible(true);
        } catch (Exception ex) {
            j7getStackTraceElementMethod = j7getStackTraceDepthMethod = null;
        }

        try {
            j8getJavaLangAccess = Class.forName("sun.misc.SharedSecrets").getDeclaredMethod("getStackTraceElement");
            j8getJavaLangAccess.setAccessible(true);
            j8getStackTraceElementMethod = Class.forName("sun.misc.JavaLangAccess").getDeclaredMethod("getStackTraceDepth", Throwable.class, int.class);
            j8getStackTraceElementMethod.setAccessible(true);
        } catch (Exception ex) {
            j8getJavaLangAccess = j8getStackTraceElementMethod = null;
        }
    }

    /**
     * If you only need one stack trace element this is faster than
     * Throwable.getStackTrace()[element], it doesn't generate the full stack
     * trace.
     */
    public static StackTraceElement getStackTraceElement(int index) {
        try {
            Throwable dummy = new Throwable();

            if (j8getStackTraceElementMethod != null) {
                return (StackTraceElement) j8getStackTraceElementMethod.invoke(j8getJavaLangAccess.invoke(null), dummy, index);
            }

//			if (JAVA_VERSION >= 9) {
//				return StackWalker.getInstance(Collections.emptySet(), index + 1)
//				.walk(s -> s.skip(index).findFirst())
//				.orElse(null);
//            }

            if (j7getStackTraceElementMethod == null) {
                // better than nothing, right? :/
                return (new Throwable()).getStackTrace()[index];
            }

            if (index < (Integer) j7getStackTraceDepthMethod.invoke(dummy)) {
                return (StackTraceElement) j7getStackTraceElementMethod.invoke(new Throwable(), index);
            }
        } catch (Throwable ignore) {
        }

        return null;
    }

    public static <T extends Annotation> Map<Class<?>, T> getClassesInClassPackageByAnnotation(Class<?> clazz, Class<T> annotation) throws IOException {
        final Map<Class<?>, T> foundClasses = new HashMap<>();

        for (Class<?> c : getAllClassesInClassPackage(clazz, false)) {
            T t = c.getAnnotation(annotation);

            if (t != null) {
                foundClasses.put(c, t);
            }
        }

        return foundClasses;
    }

    public static List<Class<?>> getAllClassesInClassPackage(Class<?> clazz, boolean recursive) throws IOException {
        final List<Class<?>> packageClasses = new ArrayList<>();

        final String clazzPackageName = clazz.getPackage().getName();
        URL dot = clazz.getResource(".");

        if (dot == null) {
            // jar file
            String packagePath = clazzPackageName.replace('.', '/');
            CodeSource src = clazz.getProtectionDomain().getCodeSource();

            if (src != null) {
                URL jar = src.getLocation();
                ZipInputStream zip = new ZipInputStream(jar.openStream());

                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    String name = e.getName();

                    if (!name.endsWith("/") && name.startsWith(packagePath + "/")) {
                        if (recursive || name.indexOf('/', packagePath.length() + 1) == -1) {
                            try {
                                Class<?> loadedClazz = Class.forName(name.substring(0, name.lastIndexOf('.')).replace('/', '.'));
                                packageClasses.add(loadedClazz);
                            } catch (ClassNotFoundException e1) {
                                SongodaCore.getLogger().log(Level.FINE, "class not found: " + e1.getMessage());
                            }
                        }
                    }
                }
            }

            return packageClasses;
        }

        String clazzPath = clazz.getResource(".").getPath();
        if (clazzPath.startsWith("/") && system_os.contains("win")) {
            clazzPath = clazzPath.substring(1);
        }
        Path packagePath = Paths.get(clazzPath);

        Files.walkFileTree(packagePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String filename = file.getName(file.getNameCount() - 1).toString();

                if (filename.endsWith(".class")) {
                    String className = filename.replace(".class", "");

                    try {
                        Class<?> loadedClazz = Class.forName(
                                clazzPackageName + "." + className);

                        packageClasses.add(loadedClazz);
                    } catch (ClassNotFoundException e) {
                        SongodaCore.getLogger().log(Level.FINE, "class not found: " + e.getMessage());
                    }
                }

                return super.visitFile(file, attrs);
            }
        });

        return packageClasses;
    }

    public enum ITERATION {
        NONE, CLASS, PACKAGE, FULL
    }

    public static List<String> getClassNamesFromPackage(Class classInPackage) throws IOException, URISyntaxException, ClassNotFoundException {
        String classPath = classInPackage.getName();
        int packageDelim = classPath.lastIndexOf('.');

        return getClassNamesFromPackage(getJarFile(classInPackage), classPath.substring(0, packageDelim), ITERATION.NONE);
    }

    public static List<String> getClassNamesFromPackage(String packageName) throws IOException, URISyntaxException, ClassNotFoundException {
        return getClassNamesFromPackage(packageName, ITERATION.NONE);
    }

    public static List<String> getClassNamesFromPackage(String packageName, ITERATION iterate) throws IOException, URISyntaxException, ClassNotFoundException {
        return getClassNamesFromPackage(null, packageName, iterate);
    }

    public static List<String> getClassNamesFromPackage(File sourceJar, String packageName, ITERATION iterate) throws IOException, URISyntaxException, ClassNotFoundException {
        // http://stackoverflow.com/questions/1456930/how-do-i-read-all-classes-from-a-java-package-in-the-classpath
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<>();

        if (packageName.contains("/")) {
            // load as a file
            packageURL = classLoader.getResource(packageName);

            // todo - if there is an error, step backwards to find the first avaliable package
            if (packageURL == null && packageName.contains("/")) {
                // added - check to see if maybe trying to load a file?
                final int i = packageName.lastIndexOf('/');
                packageName = packageName.substring(0, i) + "." + packageName.substring(i + 1);
                packageURL = classLoader.getResource(packageName);
            }
        } else {
            packageName = packageName.replace(".", "/");
            packageURL = classLoader.getResource(packageName);

            if (sourceJar == null && packageURL == null) {
                throw new IOException("Cannot open resource '" + packageName + "'");
            }
        }

        if (sourceJar == null && packageURL == null) {
            throw new IOException("Cannot open resource '" + packageName + "'");
            //} else if (packageURL.getProtocol().equals("file") || ) {
            // cannot do this..
        } else if (sourceJar != null || packageURL.getProtocol().equals("jar")) {
            // this can also be used to load jar from resources
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            // build jar file name, then loop through zipped entries
            jarFileName = sourceJar != null ? sourceJar.getAbsolutePath() : URLDecoder.decode(packageURL.getFile(), "UTF-8");
            // changed - support for resource jar files, too
            if (jarFileName.startsWith("file:/")) {
                jarFileName = jarFileName.substring(system_os.contains("win") ? 5 : 4);
            }
            if (jarFileName.startsWith("/") && system_os.contains("win")) {
                jarFileName = jarFileName.substring(1);
            }
            if (jarFileName.contains("!")) {
                jarFileName = jarFileName.substring(0, jarFileName.indexOf("!"));
            }

            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();

            // in case of multiple sub-classes, keep track of what classes have been searched
            ArrayList<String> loaded = new ArrayList<>();

            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();

                if (entryName.startsWith(packageName) && entryName.length() > packageName.length() && entryName.toLowerCase().endsWith(".class")) {
                    if (entryName.contains(".")) {
                        entryName = entryName.substring(packageName.length() + 1, entryName.lastIndexOf('.'));
                    }

                    // iteration test
                    if (!entryName.contains("/") || (iterate == ITERATION.PACKAGE || iterate == ITERATION.FULL)) {
                        if (entryName.contains("$")) { // added - sub-package test
                            // added - iteration
                            if (iterate == ITERATION.CLASS || iterate == ITERATION.FULL) {
                                entryName = entryName.substring(0, entryName.indexOf('$')).replace('/', '.');
                                if (!loaded.contains(entryName)) {
                                    loaded.add(entryName);

                                    try {
                                        Class c = Class.forName(packageName.replace('/', '.') + "." + entryName);

                                        for (Class c2 : c.getDeclaredClasses()) {
                                            names.add(entryName + "." + c2.getSimpleName());
                                        }
                                    } catch (Throwable ignore) {
                                    }
                                }
                            }
                        } else {
                            names.add(entryName.replace('/', '.'));
                        }
                    }
                }
            }
        } else {
            // hits here if running in IDE

            // loop through files in classpath
            URI uri = new URI(packageURL.toString());
            File folder = new File(uri.getPath());

            // won't work with path which contains blank (%20)
            // File folder = new File(packageURL.getFile());
            File[] contenuti = folder.listFiles();

            // in case of multiple sub-classes, keep track of what classes have been searched
            ArrayList<String> loaded = new ArrayList<>();

            String entryName;
            for (File actual : contenuti) {
                entryName = actual.getName();
                if (entryName.contains(".")) { // added - folder check
                    entryName = entryName.substring(0, entryName.lastIndexOf('.'));

                    if (entryName.contains("$")) { // added - sub-package test
                        // added - iteration
                        if (iterate == ITERATION.CLASS || iterate == ITERATION.FULL) {
                            entryName = entryName.substring(0, entryName.indexOf('$'));
                            if (!loaded.contains(entryName)) {
                                loaded.add(entryName);

                                Class c = Class.forName(packageName.replace('/', '.') + "." + entryName);
                                for (Class c2 : c.getDeclaredClasses()) {
                                    names.add(entryName + "." + c2.getSimpleName());
                                }
                            }
                        }
                    } else {
                        names.add(entryName);
                    }
                } else if (iterate == ITERATION.PACKAGE || iterate == ITERATION.FULL) {
                    // added - iteration
                    for (String sub : getClassNamesFromPackage(packageName + "/" + entryName, iterate)) {
                        names.add(entryName + "." + sub);
                    }
                }
            }
        }

        return names;
    }
}
