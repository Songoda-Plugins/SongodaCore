package com.songoda.core.locale;

import com.songoda.core.configuration.Config;
import com.songoda.core.configuration.ConfigSection;
import com.songoda.core.utils.TextUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Assists in the utilization of localization files.
 */
public class Locale {
    private static final Pattern OLD_NODE_PATTERN = Pattern.compile("^([^ ]+)\\s*=\\s*\"?(.*?)\"?$");
    private static final String FILE_EXTENSION = ".lang";

    private final Map<String, String> nodes = new HashMap<>();
    private final Plugin plugin;
    private final File file;
    private final String name;

    /**
     * Instantiate the Locale class for future use
     *
     * @param plugin Owning Plugin
     * @param file   Location of the locale file
     * @param name   The locale name for the language
     */
    public Locale(Plugin plugin, File file, String name) {
        this.plugin = plugin;
        this.file = file;
        this.name = name;
    }

    /**
     * Load a default-included lang file from the plugin's jar file
     *
     * @param plugin plugin to load from
     * @param name   name of the default locale, eg "en_US"
     *
     * @return returns the loaded Locale, or null if there was an error
     */
    public static Locale loadDefaultLocale(JavaPlugin plugin, String name) {
        saveDefaultLocale(plugin, name, name);

        return loadLocale(plugin, name);
    }

    /**
     * Load a locale from this plugin's locale directory
     *
     * @param plugin plugin to load from
     * @param name   name of the locale, eg "en_US"
     *
     * @return returns the loaded Locale, or null if there was an error
     */
    public static Locale loadLocale(JavaPlugin plugin, String name) {
        File localeFolder = new File(plugin.getDataFolder(), "locales/");
        if (!localeFolder.exists()) {
            return null;
        }

        File localeFile = new File(localeFolder, name + FILE_EXTENSION);
        if (!localeFolder.exists()) {
            return null;
        }

        // found the lang file, now load it in!
        Locale l = new Locale(plugin, localeFile, name);

        if (!l.reloadMessages()) {
            return null;
        }

        plugin.getLogger().info("Loaded locale \"" + name + "\"");

        return l;
    }

    /**
     * Load all locales from this plugin's locale directory
     *
     * @param plugin plugin to load from
     *
     * @return returns the loaded Locales
     */
    public static List<Locale> loadAllLocales(JavaPlugin plugin) {
        File localeFolder = new File(plugin.getDataFolder(), "locales/");
        List<Locale> all = new ArrayList<>();

        for (File localeFile : localeFolder.listFiles()) {
            String fileName = localeFile.getName();
            if (!fileName.endsWith(FILE_EXTENSION)) {
                continue;
            }

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            if (fileName.split("_").length != 2) {
                continue;
            }

            Locale l = new Locale(plugin, localeFile, fileName);

            if (l.reloadMessages()) {
                plugin.getLogger().info("Loaded locale \"" + fileName + "\"");
                all.add(l);
            }
        }

        return all;
    }

    /**
     * Get a list of all locale files in this plugin's locale directory
     *
     * @param plugin Plugin to check for
     */
    public static List<String> getLocales(Plugin plugin) {
        File localeFolder = new File(plugin.getDataFolder(), "locales/");
        List<String> all = new ArrayList<>();

        for (File localeFile : localeFolder.listFiles()) {
            String fileName = localeFile.getName();
            if (!fileName.endsWith(FILE_EXTENSION)) {
                continue;
            }

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));

            if (fileName.split("_").length != 2) {
                continue;
            }

            all.add(fileName);
        }

        return all;
    }

    /**
     * Save a locale file from the Plugin's Resources to the locale folder
     *
     * @param plugin   plugin owning the locale file
     * @param locale   the specific locale file to save
     * @param fileName where to save the file
     *
     * @return true if the operation was successful, false otherwise
     */
    public static boolean saveDefaultLocale(JavaPlugin plugin, String locale, String fileName) {
        return saveLocale(plugin, plugin.getResource(locale + FILE_EXTENSION), fileName, true);
    }

    /**
     * Save a locale file from an InputStream to the locale folder
     *
     * @param plugin   plugin owning the locale file
     * @param in       file to save
     * @param fileName the name of the file to save
     *
     * @return true if the operation was successful, false otherwise
     */
    public static boolean saveLocale(Plugin plugin, InputStream in, String fileName) {
        return saveLocale(plugin, in, fileName, false);
    }

    private static boolean saveLocale(Plugin plugin, InputStream in, String fileName, boolean builtin) {
        if (in == null) {
            return false;
        }

        File localeFolder = new File(plugin.getDataFolder(), "locales/");
        if (!localeFolder.exists()) {
            localeFolder.mkdirs();
        }

        if (!fileName.endsWith(FILE_EXTENSION)) {
            fileName = fileName + FILE_EXTENSION;
        }

        File destinationFile = new File(localeFolder, fileName);
        if (destinationFile.exists()) {
            return updateFiles(plugin, in, destinationFile, builtin);
        }

        try {
            Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            return fileName.split("_").length == 2;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    // Write new changes to existing files, if any at all
    private static boolean updateFiles(Plugin plugin, InputStream defaultFile, File existingFile, boolean builtin) {
        try (BufferedInputStream defaultIn = new BufferedInputStream(defaultFile);
             BufferedInputStream existingIn = new BufferedInputStream(Files.newInputStream(existingFile.toPath()))) {

            Charset defaultCharset = TextUtils.detectCharset(defaultIn, StandardCharsets.UTF_8);
            Charset existingCharset = TextUtils.detectCharset(existingIn, StandardCharsets.UTF_8);

            try (BufferedReader defaultReaderOriginal = new BufferedReader(new InputStreamReader(defaultIn, defaultCharset));
                 BufferedReader existingReaderOriginal = new BufferedReader(new InputStreamReader(existingIn, existingCharset));
                 BufferedReader defaultReader = translatePropertyToYAML(defaultReaderOriginal, defaultCharset);
                 BufferedReader existingReader = translatePropertyToYAML(existingReaderOriginal, existingCharset)) {

                Config existingLang = new Config(existingFile);
                existingLang.load(existingReader);
                translateMsgRoot(existingLang, existingFile, existingCharset);

                Config defaultLang = new Config();
                String defaultData = defaultReader.lines().map(s -> s.replaceAll("[\uFEFF\uFFFE\u200B]", "")).collect(Collectors.joining("\n"));
                defaultLang.loadFromString(defaultData);
                translateMsgRoot(defaultLang, defaultData, defaultCharset);

                List<String> added = new ArrayList<>();

                for (String defaultValueKey : defaultLang.getKeys(true)) {
                    Object val = defaultLang.get(defaultValueKey);
                    if (val instanceof ConfigSection) {
                        continue;
                    }

                    if (!existingLang.contains(defaultValueKey)) {
                        added.add(defaultValueKey);
                        existingLang.set(defaultValueKey, val);
                    }
                }

                if (!added.isEmpty()) {
                    if (!builtin) {
                        existingLang.setHeader("New messages added for " + plugin.getName() + " v" + plugin.getDescription().getVersion() + ".",
                                "",
                                "These translations were found untranslated, join",
                                "our translation Discord https://discord.gg/f7fpZEf",
                                "to request an official update!",
                                "",
                                String.join("\n", added)
                        );
                    } else {
                        existingLang.setHeader("New messages added for " + plugin.getName() + " v" + plugin.getDescription().getVersion() + ".",
                                "",
                                String.join("\n", added)
                        );
                    }

                    existingLang.setRootNodeSpacing(0);
                    existingLang.save();
                }

                return !added.isEmpty();
            } catch (InvalidConfigurationException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error checking config " + existingFile.getName(), ex);
            }
        } catch (IOException ignore) {
        }

        return false;
    }

    /**
     * Clear the previous message cache and load new messages directly from file
     *
     * @return reload messages from file
     */
    public boolean reloadMessages() {
        if (!this.file.exists()) {
            this.plugin.getLogger().warning("Could not find file for locale \"" + this.name + "\"");
            return false;
        }

        this.nodes.clear(); // Clear previous data (if any)

        // guess what encoding this file is in
        Charset charset = TextUtils.detectCharset(this.file, null);
        if (charset == null) {
            this.plugin.getLogger().warning("Could not determine charset for locale \"" + this.name + "\"");
            charset = StandardCharsets.UTF_8;
        }

        // load in the file!
        try (FileInputStream stream = new FileInputStream(this.file);
             BufferedReader source = new BufferedReader(new InputStreamReader(stream, charset));
             BufferedReader reader = translatePropertyToYAML(source, charset)) {
            Config lang = new Config(this.file);
            lang.load(reader);
            translateMsgRoot(lang, this.file, charset);

            // load lists as strings with newlines
            lang.getValues(true).forEach((k, v) -> this.nodes.put(k,
                    v instanceof List
                            ? (((List<?>) v).stream().map(Object::toString).collect(Collectors.joining("\n")))
                            : v.toString()));

            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InvalidConfigurationException ex) {
            Logger.getLogger(Locale.class.getName()).log(Level.SEVERE, "Configuration error in language file \"" + this.file.getName() + "\"", ex);
        }

        return false;
    }

    protected static BufferedReader translatePropertyToYAML(BufferedReader source, Charset charset) throws IOException {
        StringBuilder output = new StringBuilder();

        String line, line1;
        for (int lineNumber = 0; (line = source.readLine()) != null; ++lineNumber) {
            if (lineNumber == 0) {
                // remove BOM markers, if any
                line1 = line;
                line = line.replaceAll("[\uFEFF\uFFFE\u200B]", "");
                if (line1.length() != line.length()) {
                    output.append(line1, 0, line1.length() - line.length());
                }
            }

            Matcher matcher;
            if ((line = line.replace('\r', ' ')
                    .replaceAll("\\p{C}", "?")
                    .replace(";", "")).trim().isEmpty()
                    || line.trim().startsWith("#") /* Comment */
                    // need to trim the search group because tab characters somehow ended up at the end of lines in a lot of these files
                    || !(matcher = OLD_NODE_PATTERN.matcher(line.trim())).find()) {
                if (line.startsWith("//")) {
                    // someone used an improper comment in some files *grumble grumble*
                    output.append("#").append(line).append("\n");
                } else {
                    output.append(line).append("\n");
                }
            } else {
                output.append(matcher.group(1)).append(": \"").append(matcher.group(2)).append("\"\n");
            }
        }

        // I hate Java sometimes because of crap like this:
        return new BufferedReader(new InputStreamReader(new BufferedInputStream(new ByteArrayInputStream(output.toString().getBytes(charset))), charset));
    }

    protected static void translateMsgRoot(Config lang, File file, Charset charset) throws IOException {
        List<String> msgs = lang.getValues(true).entrySet().stream()
                .filter(e -> e.getValue() instanceof ConfigSection)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!msgs.isEmpty()) {
            try (FileInputStream stream = new FileInputStream(file);
                 BufferedReader source = new BufferedReader(new InputStreamReader(stream, charset))) {
                String line;
                for (int lineNumber = 0; (line = source.readLine()) != null; ++lineNumber) {
                    if (lineNumber == 0) {
                        // remove BOM markers, if any
                        line = line.replaceAll("[\uFEFF\uFFFE\u200B]", "");
                    }

                    Matcher matcher;
                    if (!(line = line.trim()).isEmpty() && !line.startsWith("#")
                            && (matcher = OLD_NODE_PATTERN.matcher(line)).find()
                            && msgs.contains(matcher.group(1))) {
                        lang.set(matcher.group(1) + ".message", matcher.group(2));
                    }
                }
            }
        }
    }

    protected static void translateMsgRoot(Config lang, String file, Charset charset) throws IOException {
        List<String> msgs = lang.getValues(true).entrySet().stream()
                .filter(e -> e.getValue() instanceof ConfigSection)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!msgs.isEmpty()) {
            String[] source = file.split("\n");

            String line;
            for (int lineNumber = 0; lineNumber < source.length; ++lineNumber) {
                line = source[lineNumber];
                if (lineNumber == 0) {
                    // remove BOM markers, if any
                    line = line.replaceAll("[\uFEFF\uFFFE\u200B]", "");
                }

                Matcher matcher;
                if (!(line = line.trim()).isEmpty() && !line.startsWith("#")
                        && (matcher = OLD_NODE_PATTERN.matcher(line)).find()
                        && msgs.contains(matcher.group(1))) {
                    lang.set(matcher.group(1) + ".message", matcher.group(2));
                }
            }
        }
    }

    /**
     * Supply the Message object with the plugins prefix.
     *
     * @param message message to be applied
     *
     * @return applied message
     */
    private Message supplyPrefix(Message message) {
        return message.setPrefix(this.nodes.getOrDefault("general.nametag.prefix", "[" + this.plugin.getName() + "]"));
    }

    /**
     * Create a new unsaved Message
     *
     * @param message the message to create
     *
     * @return the created message
     */
    public Message newMessage(String message) {
        return supplyPrefix(new Message(message));
    }

    /**
     * Get a message set for a specific node.
     *
     * @param node the node to get
     *
     * @return the message for the specified node
     */
    public Message getMessage(String node) {
        if (this.nodes.containsKey(node + ".message")) {
            node += ".message";
        }

        return this.getMessageOrDefault(node, node);
    }

    /**
     * Get a message set for a specific node
     *
     * @param node         the node to get
     * @param defaultValue the default value given that a value for the node was not found
     *
     * @return the message for the specified node. Default if none found
     */
    public Message getMessageOrDefault(String node, String defaultValue) {
        if (this.nodes.containsKey(node + ".message")) {
            node += ".message";
        }

        return supplyPrefix(new Message(this.nodes.getOrDefault(node, defaultValue)));
    }

    /**
     * Return the locale name (i.e. "en_US")
     *
     * @return the locale name
     */
    public String getName() {
        return this.name;
    }
}
