package com.songoda.core.configuration;

import com.songoda.core.utils.TextUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configuration settings for a plugin
 */
public class Config extends ConfigSection {
    /*
    Serialization notes:
    // implements ConfigurationSerializable:
    //public Map<String, Object> serialize();

    // Class must contain one of:
    // public static Object deserialize(@NotNull Map<String, ?> args);
    // public static valueOf(Map<String, ?> args);
    // public new (Map<String, ?> args)
     */
    protected static final String BLANK_CONFIG = "{}\n";

    protected File file;
    protected final ConfigFileConfigurationAdapter config = new ConfigFileConfigurationAdapter(this);
    protected Comment headerComment = null;
    protected Comment footerComment = null;
    final String dirName, fileName;
    final Plugin plugin;
    final DumperOptions yamlOptions = new DumperOptions();
    final Representer yamlRepresenter = new YamlRepresenter();
    final Yaml yaml = new Yaml(new YamlConstructor(), this.yamlRepresenter, this.yamlOptions);
    Charset defaultCharset = StandardCharsets.UTF_8;
    SaveTask saveTask;
    Timer autosaveTimer;

    ////////////// Config settings ////////////////
    /**
     * save file whenever a change is made
     */
    boolean autosave = false;

    /**
     * time in seconds to start a save after a change is made
     */
    int autosaveInterval = 60;

    /**
     * remove nodes not defined in defaults
     */
    boolean autoremove = false;

    /**
     * load comments when loading the file
     */
    boolean loadComments = true;

    /**
     * Default comment applied to config nodes
     */
    ConfigFormattingRules.CommentStyle defaultNodeCommentFormat = ConfigFormattingRules.CommentStyle.SIMPLE;

    /**
     * Default comment applied to section nodes
     */
    ConfigFormattingRules.CommentStyle defaultSectionCommentFormat = ConfigFormattingRules.CommentStyle.SPACED;

    /**
     * Extra lines to put between root nodes
     */
    int rootNodeSpacing = 1;

    /**
     * Extra lines to put in front of comments. <br>
     * This is separate from rootNodeSpacing, if applicable.
     */
    int commentSpacing = 1;

    public Config() {
        this.plugin = null;
        this.file = null;

        this.dirName = null;
        this.fileName = null;
    }

    public Config(@NotNull File file) {
        this.plugin = null;
        this.file = file.getAbsoluteFile();

        this.dirName = null;
        this.fileName = null;
    }

    public Config(@NotNull Plugin plugin) {
        this.plugin = plugin;

        this.dirName = null;
        this.fileName = null;
    }

    public Config(@NotNull Plugin plugin, @NotNull String file) {
        this.plugin = plugin;

        this.dirName = null;
        this.fileName = file;
    }

    public Config(@NotNull Plugin plugin, @Nullable String directory, @NotNull String file) {
        this.plugin = plugin;

        this.dirName = directory;
        this.fileName = file;
    }

    @NotNull
    public ConfigFileConfigurationAdapter getFileConfig() {
        return this.config;
    }

    @NotNull
    public File getFile() {
        if (this.file == null) {
            if (this.dirName != null) {
                this.file = new File(this.plugin.getDataFolder() + this.dirName, this.fileName != null ? this.fileName : "config.yml");
            } else {
                this.file = new File(this.plugin.getDataFolder(), this.fileName != null ? this.fileName : "config.yml");
            }
        }

        return this.file;
    }

    public Charset getDefaultCharset() {
        return this.defaultCharset;
    }

    /**
     * Set the Charset that will be used to save this config
     *
     * @param defaultCharset Charset to use
     *
     * @return this class
     */
    public Config setDefaultCharset(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }

    /**
     * Set the default charset to use UTF-16
     *
     * @return this class
     */
    public Config setUseUTF16() {
        this.defaultCharset = StandardCharsets.UTF_16;
        return this;
    }

    public boolean getLoadComments() {
        return this.loadComments;
    }

    /**
     * Should comments from the config file be loaded when loading?
     *
     * @param loadComments set to false if you do not want to preserve node comments
     */
    public void setLoadComments(boolean loadComments) {
        this.loadComments = loadComments;
    }

    public boolean getAutosave() {
        return this.autosave;
    }

    /**
     * Should the configuration automatically save whenever it's been changed? <br>
     * All saves are done asynchronously, so this should not impact server performance.
     *
     * @param autosave set to true if autosaving is enabled.
     *
     * @return this class
     */
    @NotNull
    public Config setAutosave(boolean autosave) {
        this.autosave = autosave;
        return this;
    }

    public int getAutosaveInterval() {
        return this.autosaveInterval;
    }

    /**
     * If autosave is enabled, this is the delay between a change and when the save is started. <br>
     * If the configuration is changed within this period, the timer is not reset.
     *
     * @param autosaveInterval time in seconds
     *
     * @return this class
     */
    @NotNull
    public Config setAutosaveInterval(int autosaveInterval) {
        this.autosaveInterval = autosaveInterval;
        return this;
    }

    public boolean getAutoremove() {
        return this.autoremove;
    }

    /**
     * This setting is used to prevent users to from adding extraneous settings
     * to the config and to remove deprecated settings. <br>
     * If this is enabled, the config will delete any nodes that are not defined
     * as a default setting.
     *
     * @param autoremove Remove settings that don't exist as defaults
     *
     * @return this class
     */
    @NotNull
    public Config setAutoremove(boolean autoremove) {
        this.autoremove = autoremove;
        return this;
    }

    /**
     * Default comment applied to config nodes
     */
    @Nullable
    public ConfigFormattingRules.CommentStyle getDefaultNodeCommentFormat() {
        return this.defaultNodeCommentFormat;
    }

    /**
     * Default comment applied to config nodes
     *
     * @return this config
     */
    @NotNull
    public Config setDefaultNodeCommentFormat(@Nullable ConfigFormattingRules.CommentStyle defaultNodeCommentFormat) {
        this.defaultNodeCommentFormat = defaultNodeCommentFormat;
        return this;
    }

    /**
     * Default comment applied to section nodes
     */
    @Nullable
    public ConfigFormattingRules.CommentStyle getDefaultSectionCommentFormat() {
        return this.defaultSectionCommentFormat;
    }

    /**
     * Default comment applied to section nodes
     *
     * @return this config
     */
    @NotNull
    public Config setDefaultSectionCommentFormat(@Nullable ConfigFormattingRules.CommentStyle defaultSectionCommentFormat) {
        this.defaultSectionCommentFormat = defaultSectionCommentFormat;
        return this;
    }

    /**
     * Extra lines to put between root nodes
     */
    public int getRootNodeSpacing() {
        return this.rootNodeSpacing;
    }

    /**
     * Extra lines to put between root nodes
     *
     * @return this config
     */
    @NotNull
    public Config setRootNodeSpacing(int rootNodeSpacing) {
        this.rootNodeSpacing = rootNodeSpacing;
        return this;
    }

    /**
     * Extra lines to put in front of comments. <br>
     * This is separate from rootNodeSpacing, if applicable.
     */
    public int getCommentSpacing() {
        return this.commentSpacing;
    }

    /**
     * Extra lines to put in front of comments. <br>
     * This is separate from rootNodeSpacing, if applicable.
     *
     * @return this config
     */
    @NotNull
    public Config setCommentSpacing(int commentSpacing) {
        this.commentSpacing = commentSpacing;
        return this;
    }

    @NotNull
    public Config setHeader(@NotNull String... description) {
        if (description.length == 0) {
            this.headerComment = null;
        } else {
            this.headerComment = new Comment(description);
        }

        return this;
    }

    @NotNull
    public Config setHeader(@Nullable ConfigFormattingRules.CommentStyle commentStyle, @NotNull String... description) {
        if (description.length == 0) {
            this.headerComment = null;
        } else {
            this.headerComment = new Comment(commentStyle, description);
        }

        return this;
    }

    @NotNull
    public Config setHeader(@Nullable List<String> description) {
        if (description == null || description.isEmpty()) {
            this.headerComment = null;
        } else {
            this.headerComment = new Comment(description);
        }

        return this;
    }

    @NotNull
    public Config setHeader(@Nullable ConfigFormattingRules.CommentStyle commentStyle, @Nullable List<String> description) {
        if (description == null || description.isEmpty()) {
            this.headerComment = null;
        } else {
            this.headerComment = new Comment(commentStyle, description);
        }

        return this;
    }

    @NotNull
    public List<String> getHeader() {
        if (this.headerComment != null) {
            return this.headerComment.getLines();
        }

        return Collections.emptyList();
    }

    public Config clearConfig(boolean clearDefaults) {
        this.root.values.clear();
        this.root.configComments.clear();

        if (clearDefaults) {
            this.root.defaultComments.clear();
            this.root.defaults.clear();
        }

        return this;
    }

    public Config clearDefaults() {
        this.root.defaultComments.clear();
        this.root.defaults.clear();

        return this;
    }

    public boolean load() {
        return load(getFile());
    }

    public boolean load(@NotNull File file) {
        Validate.notNull(file, "File cannot be null");
        if (file.exists()) {
            try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
                Charset charset = TextUtils.detectCharset(stream, StandardCharsets.UTF_8);

                // upgrade charset if file was saved in a more complex format
                if (charset == StandardCharsets.UTF_16BE || charset == StandardCharsets.UTF_16LE) {
                    this.defaultCharset = StandardCharsets.UTF_16;
                }

                this.load(new InputStreamReader(stream, charset));

                return true;
            } catch (IOException | InvalidConfigurationException ex) {
                (this.plugin != null ? this.plugin.getLogger() : Bukkit.getLogger()).log(Level.SEVERE, "Failed to load config file: " + file.getName(), ex);
            }

            return false;
        }

        return true;
    }

    public void load(@NotNull Reader reader) throws IOException, InvalidConfigurationException {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)) {
            String line;
            boolean firstLine = true;
            while ((line = input.readLine()) != null) {
                if (firstLine) {
                    line = line.replaceAll("[\uFEFF\uFFFE\u200B]", ""); // clear BOM markers
                    firstLine = false;
                }
                builder.append(line).append('\n');
            }
        }

        this.loadFromString(builder.toString());
    }

    public void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
        Map<?, ?> input;

        try {
            input = this.yaml.load(contents);
        } catch (YAMLException e2) {
            throw new InvalidConfigurationException(e2);
        } catch (ClassCastException e3) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        if (input != null) {
            if (this.loadComments) {
                this.parseComments(contents, input);
            }

            this.convertMapsToSections(input, this);
        }
    }

    protected void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull ConfigSection section) {
        // TODO: make this non-recursive
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
                continue;
            }

            section.set(key, value);
        }
    }

    protected void parseComments(@NotNull String contents, @NotNull Map<?, ?> input) {
        // if starts with a comment, load all nonbreaking comments as a header
        // then load all comments and assign to the next valid node loaded
        // (Only load comments that are on their own line)

        BufferedReader in = new BufferedReader(new StringReader(contents));
        String line;
        boolean insideScalar = false;
        boolean firstNode = true;
        int index = 0;
        LinkedList<String> currentPath = new LinkedList<>();
        ArrayList<String> commentBlock = new ArrayList<>();

        try {
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) {
                    if (firstNode && !commentBlock.isEmpty()) {
                        // header comment
                        firstNode = false;
                        this.headerComment = Comment.loadComment(commentBlock);
                        commentBlock.clear();
                    }
                    continue;
                } else if (line.trim().startsWith("#")) {
                    // only load full-line comments
                    commentBlock.add(line.trim());
                    continue;
                }

                // check to see if this is a line that we can process
                int lineOffset = getOffset(line);
                insideScalar &= lineOffset <= index;
                Matcher m;
                if (!insideScalar && (m = this.yamlNode.matcher(line)).find()) {
                    // we found a config node! ^.^
                    // check to see what the full path is
                    int depth = (m.group(1).length() / this.indentation);
                    while (depth < currentPath.size()) {
                        currentPath.removeLast();
                    }
                    currentPath.add(m.group(2));

                    // do we have a comment for this node?
                    if (!commentBlock.isEmpty()) {
                        String path = currentPath.stream().collect(Collectors.joining(String.valueOf(this.pathChar)));
                        Comment comment = Comment.loadComment(commentBlock);
                        commentBlock.clear();
                        setComment(path, comment);
                    }

                    firstNode = false; // we're no longer on the first node

                    // ignore scalars
                    index = lineOffset;
                    if (m.group(3).trim().equals("|") || m.group(3).trim().equals(">")) {
                        insideScalar = true;
                    }
                }
            }

            if (!commentBlock.isEmpty()) {
                this.footerComment = Comment.loadComment(commentBlock);
                commentBlock.clear();
            }
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, "Error parsing config comment", ex);
        }
    }

    public void deleteNonDefaultSettings() {
        // Delete old config values (thread-safe)
        List<String> defaultKeys = Arrays.asList(this.defaults.keySet().toArray(new String[0]));

        for (String key : this.values.keySet().toArray(new String[0])) {
            if (!defaultKeys.contains(key)) {
                this.values.remove(key);
            }
        }
    }

    @Override
    protected void onChange() {
        if (this.autosave) {
            delaySave();
        }
    }

    public void delaySave() {
        // save async even if no plugin or if plugin disabled
        if (this.saveTask == null && (this.changed || hasNewDefaults())) {
            this.autosaveTimer = new Timer((this.plugin != null ? this.plugin.getName() + "-ConfigSave-" : "ConfigSave-") + getFile().getName());
            this.autosaveTimer.schedule(this.saveTask = new SaveTask(), this.autosaveInterval * 1000L);
        }
    }

    public boolean saveChanges() {
        boolean saved = true;

        if (this.changed || hasNewDefaults()) {
            saved = save();
        }

        if (this.saveTask != null) {
            //Close Threads
            this.saveTask.cancel();
            this.autosaveTimer.cancel();
            this.saveTask = null;
            this.autosaveTimer = null;
        }

        return saved;
    }

    boolean hasNewDefaults() {
        if (this.file != null && !this.file.exists()) return true;

        for (String def : this.defaults.keySet()) {
            if (!this.values.containsKey(def)) {
                return true;
            }
        }

        return false;
    }

    public boolean save() {
        if (this.saveTask != null) {
            //Close Threads
            this.saveTask.cancel();
            this.autosaveTimer.cancel();
            this.saveTask = null;
            this.autosaveTimer = null;
        }

        return save(getFile());
    }

    public boolean save(@NotNull String file) {
        Validate.notNull(file, "File cannot be null");
        return this.save(new File(file));
    }

    public boolean save(@NotNull File file) {
        Validate.notNull(file, "File cannot be null");

        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        String data = this.saveToString();
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), this.defaultCharset)) {
            writer.write(data);
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    @NotNull
    public String saveToString() {
        try {
            if (this.autoremove) {
                deleteNonDefaultSettings();
            }

            this.yamlOptions.setIndent(this.indentation);
            this.yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            this.yamlOptions.setSplitLines(false);
            this.yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            StringWriter str = new StringWriter();

            if (this.headerComment != null) {
                this.headerComment.writeComment(str, 0, ConfigFormattingRules.CommentStyle.BLOCKED);
                str.write("\n"); // add one space after the header
            }

            String dump = this.yaml.dump(this.getValues(false));
            if (!dump.equals(BLANK_CONFIG)) {
                writeComments(dump, str);
            }

            if (this.footerComment != null) {
                str.write("\n");
                this.footerComment.writeComment(str, 0, ConfigFormattingRules.CommentStyle.BLOCKED);
            }

            return str.toString();
        } catch (Throwable ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, "Error saving config", ex);
            delaySave();
        }

        return "";
    }

    protected final Pattern yamlNode = Pattern.compile("^( *)([^:{}\\[\\],&*#?|\\-<>=!%@`]+):(.*)$");

    protected void writeComments(String data, Writer out) throws IOException {
        // line-by-line apply line spacing formatting and comments per-node
        BufferedReader in = new BufferedReader(new StringReader(data));

        String line;
        boolean insideScalar = false;
        boolean firstNode = true;
        int index = 0;

        LinkedList<String> currentPath = new LinkedList<>();
        while ((line = in.readLine()) != null) {
            // ignore comments and empty lines (there shouldn't be any, but just in case)
            if (line.trim().startsWith("#") || line.isEmpty()) {
                continue;
            }

            // check to see if this is a line that we can process
            int lineOffset = getOffset(line);
            insideScalar &= lineOffset <= index;
            Matcher m;
            if (!insideScalar && (m = this.yamlNode.matcher(line)).find()) {
                // we found a config node! ^.^
                // check to see what the full path is
                int depth = (m.group(1).length() / this.indentation);
                while (depth < currentPath.size()) {
                    currentPath.removeLast();
                }

                currentPath.add(m.group(2));
                String path = currentPath.stream().collect(Collectors.joining(String.valueOf(this.pathChar)));

                // if this is a root-level node, apply extra spacing if we aren't the first node
                if (!firstNode && depth == 0 && this.rootNodeSpacing > 0) {
                    out.write((new String(new char[this.rootNodeSpacing])).replace("\0", "\n")); // yes it's silly, but it works :>
                }
                firstNode = false; // we're no longer on the first node

                // insert the relavant comment
                Comment comment = getComment(path);
                if (comment != null) {
                    // add spacing between previous nodes and comments
                    if (depth != 0) {
                        out.write((new String(new char[this.commentSpacing])).replace("\0", "\n"));
                    }

                    // formatting style for this node
                    ConfigFormattingRules.CommentStyle style = comment.getCommentStyle();
                    if (style == null) {
                        // check to see what type of node this is
                        if (!m.group(3).trim().isEmpty()) {
                            // setting node
                            style = this.defaultNodeCommentFormat;
                        } else {
                            // probably a section? (need to peek ahead to check if this is a list)
                            in.mark(1000);
                            String nextLine = in.readLine().trim();
                            in.reset();
                            if (nextLine.startsWith("-")) {
                                // not a section :P
                                style = this.defaultNodeCommentFormat;
                            } else {
                                style = this.defaultSectionCommentFormat;
                            }
                        }
                    }

                    // write it down!
                    comment.writeComment(out, lineOffset, style);
                }

                // ignore scalars
                index = lineOffset;
                if (m.group(3).trim().equals("|") || m.group(3).trim().equals(">")) {
                    insideScalar = true;
                }
            }

            out.write(line);
            out.write("\n");
        }
    }

    protected static int getOffset(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] != ' ') {
                return i;
            }
        }

        return -1;
    }

    class SaveTask extends TimerTask {
        @Override
        public void run() {
            saveChanges();
        }
    }
}
