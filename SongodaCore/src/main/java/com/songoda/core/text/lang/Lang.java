package com.songoda.core.text.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.md_5.bungee.api.chat.TextComponent;
import net.vortexdevelopment.vinject.config.yaml.YamlConfig;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.gui.Gui;
import com.songoda.core.hooks.internal.ReloadHook;
import com.songoda.core.text.AdventureUtils;
import com.songoda.core.text.MiniMessagePlaceholder;
import com.songoda.core.vinject.annotation.RegisterReloadHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@net.vortexdevelopment.vinject.annotation.component.Component(priority = 1) //Ensure we load it before anything else
@RegisterReloadHook
public class Lang implements ReloadHook {

    public static List<MiniMessagePlaceholder> staticPlaceholders = new ArrayList<>();
    private static boolean initialized = false;
    private static boolean warnedNotInitialized = false;
    private static YamlConfig lang;
    private static volatile long staticPlaceholdersRevision;

    private Lang() {
        onReload();
    }

    /**
     * Changes whenever the language-level MiniMessage placeholders are rebuilt.
     * Consumers that cache rendered components can use this to invalidate them
     * without copying the placeholder list on every read.
     */
    public static long getStaticPlaceholdersRevision() {
        return staticPlaceholdersRevision;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Loads lang.yml before DI components and YAML configs are deserialized.
     * Safe to call multiple times.
     */
    public static void initializeEarly() {
        loadLanguageFile();
    }

    /**
     * Adds static placeholders to the list of placeholders.
     *
     * @param placeholders The placeholders to expand.
     * @return A list of placeholders including the static ones.
     */
    public static List<MiniMessagePlaceholder> createPlaceholders(MiniMessagePlaceholder... placeholders) {
        List<MiniMessagePlaceholder> placeholderList = new ArrayList<>(Stream.of(placeholders).toList());
        placeholderList.addAll(staticPlaceholders);
        return placeholderList;
    }

    public static String replaceStaticPlaceholders(String message) {
        for (MiniMessagePlaceholder placeholder : staticPlaceholders) {
            if (placeholder.isComponent()) continue; // Should not happen in string replacement
            message = message.replace("<" + placeholder.getPlaceholder() + ">", placeholder.getValue().toString());
        }
        return message;
    }

    public static List<String> replaceStaticPlaceholders(@NotNull List<String> list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            for (MiniMessagePlaceholder placeholder : staticPlaceholders) {
                if (placeholder.isComponent()) continue; // Should not happen in string replacement
                line = line.replace("<" + placeholder.getPlaceholder() + ">", placeholder.getValue().toString());
            }
            result.add(line);
        }
        return result;
    }

    public static String getString(String key) {
        if (!initialized) {
            warnNotInitialized();
            return key;
        }
        return replaceStaticPlaceholders(lang.getString(key, key));
    }

    public static String getString(String key, String defaultValue) {
        if (!initialized) {
            return defaultValue;
        }
        return replaceStaticPlaceholders(lang.getString(key, defaultValue));
    }

    public static Component getComponent(String key) {
        if (!initialized) {
            warnNotInitialized();
            return AdventureUtils.formatComponent(key);
        }
        return AdventureUtils.formatComponent(lang.getString(key, key), staticPlaceholders);
    }

    public static Component getComponent(String key, MiniMessagePlaceholder... placeholders) {
        if (!initialized) {
            warnNotInitialized();
            return AdventureUtils.formatComponent(key);
        }
        return AdventureUtils.formatComponent(lang.getString(key, key), createPlaceholders(placeholders));
    }

    public static List<String> getList(String key) {
        if (!initialized) {
            warnNotInitialized();
            return List.of(key);
        }
        return lang.getStringList(key);
    }

    public static void send(CommandSender player, String key) {
        if (!initialized) {
            warnNotInitialized();
            return;
        }
        AdventureUtils.sendMessage(getComponent(key), player);
    }

    public static void sendPrefixed(CommandSender player, String key) {
        if (!initialized) {
            warnNotInitialized();
            return;
        }
        AdventureUtils.sendMessage(getPrefix().append(getComponent(key)), player);
    }

    public static void send(CommandSender player, String key, MiniMessagePlaceholder... placeholders) {
        if (!initialized) {
            warnNotInitialized();
            return;
        }
        AdventureUtils.sendMessage(getComponent(key, placeholders), player);
    }

    private static String getPrefixString() {
        if (!initialized) {
            return SongodaPlugin.getInstance().getPrefixString();
        }
        return lang.getString("General.Plugin Prefix", SongodaPlugin.getInstance().getPrefixString());
    }

    public static Component getPrefix() {
        if (!initialized) {
            return SongodaPlugin.getInstance().getPrefixWithDash();
        }
        return getComponent("General.Plugin Prefix");
    }

    private static void warnNotInitialized() {
        if (warnedNotInitialized) {
            return;
        }
        warnedNotInitialized = true;
        SongodaPlugin.getInstance().getLogger().warning("Language file has not been initialized!");
    }

    public static void runConsoleCommands(@Nullable Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        //Check if we on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(SongodaPlugin.getInstance(), () -> runConsoleCommands(player, commands));
            return;
        }

        for (String command : commands) {
            command = processRandomPlaceholders(command);
            //Check if command stats with [MESSAGE], if so send the message to the player
            if (command.startsWith("[MESSAGE]")) {
                command = command.substring(9).trim();
                AdventureUtils.sendMessage(AdventureUtils.formatComponent(command, createOptionalPlaceholder(player != null, "player", player.getName())), player);
                continue;
            }

            //Check if command stats with [MESSAGE:<key>], if so send the message to the player from the lang file
            if (command.startsWith("[MESSAGE:")) {
                String key = command.substring(9, command.indexOf("]"));
                command = command.substring(command.indexOf("]") + 1).trim();
                AdventureUtils.sendMessage(getComponent(key, createOptionalPlaceholder(player != null, "player", player.getName())), player);
                continue;
            }

            // Check for sound - [SOUND] SOUND_NAME [VOLUME] [PITCH]
            if (command.startsWith("[SOUND]")) {
                command = command.substring(7).trim();
                if (player != null) {
                    try {
                        String[] parts = command.split(" ");
                        String soundName = parts[0];
                        String namespace = soundName.split(":").length > 1 ? soundName.split(":")[0] : "minecraft";
                        String key = soundName.split(":").length > 1 ? soundName.split(":")[1] : soundName;
                        Sound sound = (Sound) Sound.class.getDeclaredField(key).get(null);
                        if (sound == null) {
                            SongodaPlugin.getInstance().getLogger().warning("Sound not found: " + soundName);
                            continue;
                        }
                        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    } catch (Exception e) {
                        SongodaPlugin.getInstance().getLogger().warning("Failed to play sound: " + command);
                        e.printStackTrace();
                    }
                }
                continue;
            }

            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (player != null) {
                command = command.replace("<player>", player.getName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.trim());
        }
    }

    private static String processRandomPlaceholders(String command) {
        // [RANDOM_NUMBER:from:to]
        Pattern randomNumberPattern = Pattern.compile("\\[RANDOM_NUMBER:(-?\\d+):(-?\\d+)\\]");
        Matcher randomNumberMatcher = randomNumberPattern.matcher(command);
        StringBuilder sb = new StringBuilder();
        while (randomNumberMatcher.find()) {
            try {
                long from = Long.parseLong(randomNumberMatcher.group(1));
                long to = Long.parseLong(randomNumberMatcher.group(2));
                long min = Math.min(from, to);
                long max = Math.max(from, to);
                long randomValue = ThreadLocalRandom.current().nextLong(min, max + 1);
                randomNumberMatcher.appendReplacement(sb, String.valueOf(randomValue));
            } catch (Exception e) {
                randomNumberMatcher.appendReplacement(sb, randomNumberMatcher.group(0));
            }
        }
        randomNumberMatcher.appendTail(sb);
        command = sb.toString();

        // [RANDOM:element1:element2:...]
        Pattern randomPattern = Pattern.compile("\\[RANDOM:([^\\]]+)\\]");
        Matcher randomMatcher = randomPattern.matcher(command);
        sb = new StringBuilder();
        while (randomMatcher.find()) {
            String[] elements = randomMatcher.group(1).split(":");
            if (elements.length > 0) {
                String randomElement = elements[ThreadLocalRandom.current().nextInt(elements.length)];
                randomMatcher.appendReplacement(sb, Matcher.quoteReplacement(randomElement));
            } else {
                randomMatcher.appendReplacement(sb, "");
            }
        }
        randomMatcher.appendTail(sb);
        return sb.toString();
    }

    public static MiniMessagePlaceholder[] createOptionalPlaceholder(boolean condition, String placeholder, String value) {
        if (condition) {
            return new MiniMessagePlaceholder[]{new MiniMessagePlaceholder(placeholder, value)};
        }
        return new MiniMessagePlaceholder[0];
    }

    private static void loadLanguageFile() {
        try {
            staticPlaceholders.clear();
            File langFile = new File(SongodaPlugin.getInstance().getDataFolder(), "lang.yml");
            if (!langFile.exists()) {
                // Check if it exists in the jar
                if (SongodaPlugin.getInstance().getResource("lang.yml") == null) {
                    return;
                }
                SongodaPlugin.getInstance().saveResource("lang.yml", false);
            }
            lang = YamlConfig.load(langFile);

            // Add missing keys from bundled resource
            InputStream defConfigStream = SongodaPlugin.getInstance().getResource("lang.yml");
            if (defConfigStream != null) {
                String defContent = new String(defConfigStream.readAllBytes(), StandardCharsets.UTF_8);
                YamlConfig defConfig = YamlConfig.load(defContent);
                boolean changed = false;
                for (String key : defConfig.getKeys(true)) {
                    if (defConfig.isSection(key)) {
                        continue;
                    }
                    if (!lang.contains(key) || (lang.get(key) == null) || lang.getString(key).isEmpty()) {
                        lang.set(key, defConfig.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    lang.save();
                }
            }

            InputStream coreDefaultsStream = Lang.class.getResourceAsStream("/songodacore-default-lang.yml");
            if (coreDefaultsStream != null) {
                String coreDefaultsContent = new String(coreDefaultsStream.readAllBytes(), StandardCharsets.UTF_8);
                YamlConfig coreDefaults = YamlConfig.load(coreDefaultsContent);
                boolean changed = false;
                for (String key : coreDefaults.getKeys(true)) {
                    if (coreDefaults.isSection(key)) {
                        continue;
                    }
                    if (!lang.contains(key) || lang.get(key) == null || lang.getString(key).isEmpty()) {
                        lang.set(key, coreDefaults.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    lang.save();
                }
            }

            staticPlaceholders.add(new MiniMessagePlaceholder("prefix", lang.getString("General.Plugin Prefix", SongodaPlugin.getInstance().getPrefixString())));
            if (lang.getConfigurationSection("Colors") != null) {
                for (String placeholder : lang.getConfigurationSection("Colors").getKeys(false)) {
                    staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, "<color:" + lang.getString("Colors." + placeholder) + ">"));
                }
            }

            // Custom Placeholders
            if (lang.getConfigurationSection("Custom Placeholders") != null) {
                for (String placeholder : lang.getConfigurationSection("Custom Placeholders").getKeys(false)) {
                    staticPlaceholders.add(new MiniMessagePlaceholder(placeholder, lang.getString("Custom Placeholders." + placeholder)));
                }
            }
            initialized = true;
            Gui.BACK_BUTTON_NAME = getString("GUI.Back Button Name", "§cBack");
            AdventureUtils.reloadMiniMessage();
            staticPlaceholdersRevision++;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Resolves the locale used by a player client, falling back to English when unavailable.
     */
    public static Locale getPlayerLocale(@Nullable Player player) {
        if (player == null) {
            return Locale.ENGLISH;
        }
        try {
            Object localeValue = player.locale();
            if (localeValue instanceof Locale locale) {
                return locale;
            }
            if (localeValue instanceof String tag && !tag.isBlank()) {
                return Locale.forLanguageTag(tag.replace('_', '-'));
            }
        } catch (NoSuchMethodError | AbstractMethodError ignored) {
        }
        return Locale.ENGLISH;
    }

    /**
     * Translates a component to the player's client language when possible.
     */
    public static Component translateForPlayer(@NotNull Component component, @Nullable Player player) {
        if (player == null) {
            return component;
        }
        if (component instanceof TranslatableComponent translatable) {
            try {
                return GlobalTranslator.translator().translate(translatable, getPlayerLocale(player));
            } catch (Exception ignored) {
                return Component.text(translatable.key());
            }
        }
        return component;
    }

    /**
     * Converts a MiniMessage string to plain text, optionally using the player's client language
     * for translatable tags such as {@code <lang:item.minecraft.cake>}.
     */
    public static String toPlainText(@Nullable String miniMessage, @Nullable Player player) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return "";
        }
        Component component = AdventureUtils.formatComponent(miniMessage);
        if (player != null) {
            component = translateForPlayer(component, player);
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Translates a client key (e.g., item.minecraft.diamond_sword) to a Component for the client's language.
     *
     * @param key The client key to translate.
     * @return The translated Component.
     */
    public static Component translateClientKey(String key) {
        return Component.translatable(key);
    }

    /**
     * Translates a client key (e.g., item.minecraft.diamond_sword) to a MiniMessage formatted string for the client's language.
     *
     * @param key The client key to translate.
     * @return The translated MiniMessage string.
     */
    public static String translateClientKeyTo(String key) {
        return AdventureUtils.toMiniMessage(translateClientKey(key));
    }

    /**
     * Translates a Material to its item name in the client's language.
     *
     * @param material The Material to translate.
     * @return The translated item name.
     */
    public static String translateItemName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        return translateClientKeyTo(material.translationKey());
    }

    public static Component translateItemNameComponent(Material material) {
        if (material == null) {
            return Component.text("Unknown");
        }
        return translateClientKey(material.translationKey());
    }

    /**
     * Translates an EntityType to its mob name in the client's language.
     *
     * @param entityType The EntityType to translate.
     * @return The translated mob name.
     */
    public static String translateMobName(EntityType entityType) {
        if (entityType == null) {
            return "Unknown";
        }
        return translateClientKeyTo(entityType.translationKey());
    }

    public static Component translateMobNameComponent(EntityType entityType) {
        if (entityType == null) {
            return Component.text("Unknown");
        }
        return translateClientKey(entityType.translationKey());
    }

    /**
     * Translates a Material to its block name in the client's language.
     *
     * @param material The Material to translate.
     * @return The translated block name.
     */
    public static String translateBlockName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        return translateClientKeyTo(material.translationKey());
    }

    public static Component translateBlockNameComponent(Material material) {
        if (material == null) {
            return Component.text("Unknown");
        }
        return translateClientKey(material.translationKey());
    }

    public static TextComponent translateBlockNameTextComponent(Material material) {
        if (material == null) {
            return new TextComponent("Unknown");
        }
        return new TextComponent(translateClientKeyTo(material.translationKey()));
    }

    @Override
    public void onReload() {
        loadLanguageFile();
    }
}
