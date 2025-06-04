package com.songoda.core.chat;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.compatibility.ServerVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AdventureUtils {
    private static Method displayNameMethod = null;
    private static Method setLoreMethod = null;
    private static Method getLoreMethod = null;
    private static Object gsonComponentSerializer;
    private static Method gsonDeserializeMethod;


    static {
        if (ServerProject.isServer(ServerProject.PAPER) && ServerVersion.isServerVersionAtLeast(ServerVersion.V1_18)) {
            try {
                Class<?> componentClass = Class.forName("net;kyori;adventure;text;Component".replace(";", "."));
                displayNameMethod = ItemMeta.class.getDeclaredMethod("displayName", componentClass);
                setLoreMethod = ItemMeta.class.getDeclaredMethod("lore", List.class);
                setLoreMethod = ItemMeta.class.getDeclaredMethod("lore");
                gsonComponentSerializer = Class.forName("net;kyori;adventure;text;serializer;gson;GsonComponentSerializer".replace(";", ".")).getDeclaredMethod("gson").invoke(null);
                gsonDeserializeMethod = gsonComponentSerializer.getClass().getDeclaredMethod("deserialize", String.class);
                gsonDeserializeMethod.setAccessible(true);

            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Convert a shaded component to a json string
     *
     * @param component The shaded Component to convert
     *
     * @return Json string
     */
    public static String convertToJson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Convert a json string to the non-shaded component
     * Cast it to the correct type
     *
     * @param json Json string
     *
     * @return Non-shaded component
     */
    public static Object convertToOriginalComponent(String json) {
        try {
            return gsonDeserializeMethod.invoke(gsonComponentSerializer, json);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Convert the shaded Component to the original one
     * Cast it to the correct type
     *
     * @param component Shaded component
     *
     * @return Original component
     */
    public static Object convertToOriginalComponent(Component component) {
        try {
            return gsonDeserializeMethod.invoke(gsonComponentSerializer, convertToJson(component));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Convert a list of shaded components to a list of original components
     * Cast it to the correct type
     *
     * @param components List of shaded components
     *
     * @return List of original components
     */
    public static Object convertToOriginalComponent(List<Component> components) {
        try {
            LinkedList<Object> list = new LinkedList<>();
            for (Component component : components) {
                list.add(convertToOriginalComponent(component));
            }
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Convert a list of shaded components to a list of original components
     * Cast it to the correct type
     *
     * @param components List of shaded components
     *
     * @return List of original components
     */
    public static Object convertToOriginalComponent(Component... components) {
        try {
            LinkedList<Object> list = new LinkedList<>();
            for (Component component : components) {
                list.add(convertToOriginalComponent(component));
            }
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void sendMessage(Component message, CommandSender... target) {
        for (CommandSender sender : target) {
            SongodaCore.getSongodaPlugin().getBukkitAudiences().sender(sender).sendMessage(message);
        }
    }

    //Items
    public static void formatItemName(ItemStack item, String name) {
        formatItemName(item, formatComponent(name));
    }

    public static void formatItemLore(ItemStack item, List<String> lore) {
        formatItemLore(item, lore.toArray(new String[0]));
    }

    public static void formatItemLore(ItemStack item, String... lore) {
        formatItemLore(item, formatComponent(lore));
    }

    public static void formatItemLore(ItemStack item, List<Component> lore, String... unused) {
        formatItemLore(item, lore.toArray(new Component[0]));
    }

    public static void formatItemName(ItemStack item, Component name) {
        setItemName(item, name);
    }

    public static void formatItemLore(ItemStack item, Component... lore) {
        setItemLore(item, lore);
    }

    public static void appendItemLore(ItemStack item, List<Component> lore) {
        List<Component> currentLore = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            if (isMiniMessageEnabled()) {
                try {
                    currentLore = (List<Component>) getLoreMethod.invoke(meta);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                currentLore = formatComponent(meta.getLore());
            }
        }

        currentLore.addAll(lore);
        setItemLore(item, currentLore.toArray(new Component[0]));
    }

    public static boolean isMiniMessageEnabled() {
        return ServerProject.isServer(ServerProject.PAPER) && ServerVersion.isServerVersionAtLeast(ServerVersion.V1_16) && displayNameMethod != null && setLoreMethod != null;
    }

    private static void setItemName(ItemStack item, Component name) {
        if (name == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (isMiniMessageEnabled()) {
            //Set name as a component
            try {
                displayNameMethod.invoke(meta, convertToOriginalComponent(name));
                item.setItemMeta(meta);
                return;
            } catch (Exception ignored) {
            }
        }
        meta.setDisplayName(toLegacy(name));
        item.setItemMeta(meta);
    }

    private static void setItemLore(ItemStack item, Component... lore) {
        if (lore == null || lore.length == 0) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (isMiniMessageEnabled()) {
            //Set lore as component
            try {
                setLoreMethod.invoke(meta, convertToOriginalComponent(lore));
                item.setItemMeta(meta);
                return;
            } catch (Exception ignored) {
            }
        }
        meta.setLore(toLegacy(lore));
        item.setItemMeta(meta);
    }

    // Formatting stuff
    public static Component formatComponent(String text) {
        MiniMessage miniMessage = MiniMessage.builder().build();
        Component component = MiniMessage.miniMessage().deserialize(replaceLegacy(text));
        if (!component.hasDecoration(TextDecoration.ITALIC)) {
            component = component.decoration(TextDecoration.ITALIC, false);
        }
        return component;
    }

    public static Component formatComponent(String text, MiniMessagePlaceholder... placeholders) {
        MiniMessage miniMessage = MiniMessage.builder().editTags(builder -> {
            Arrays.stream(placeholders).forEach(placeholder ->
                    builder.resolver(Placeholder.parsed(placeholder.getPlaceholder(), placeholder.getValue()))
            );
        }).build();
        Component component = miniMessage.deserialize(replaceLegacy(text));
        if (!component.hasDecoration(TextDecoration.ITALIC)) {
            component = component.decoration(TextDecoration.ITALIC, false);
        }
        return component;
    }

    public static Component formatComponent(String text, List<MiniMessagePlaceholder> placeholders) {
        return formatComponent(text, placeholders.toArray(new MiniMessagePlaceholder[0]));
    }

    public static List<Component> formatComponent(List<String> list) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line));
        }
        return result;
    }

    public static List<Component> formatComponent(String... list) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line));
        }
        return result;
    }

    public static List<Component> formatComponent(List<String> list, MiniMessagePlaceholder... placeholders) {
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatComponent(line, placeholders));
        }
        return result;
    }

    public static List<Component> formatComponent(List<String> list, List<MiniMessagePlaceholder> placeholders) {
        return formatComponent(list, placeholders.toArray(new MiniMessagePlaceholder[0]));
    }

    public static String formatLegacy(String text) {
        return ChatColor.translateAlternateColorCodes('&',
                LegacyComponentSerializer.legacyAmpersand().serialize(MiniMessage.miniMessage().deserialize(replaceLegacy(text))));
    }

    public static List<String> formatLegacy(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatLegacy(line));
        }
        return result;
    }

    public static List<String> formatLegacy(String... list) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(formatLegacy(line));
        }
        return result;
    }

    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static List<String> toLegacy(List<Component> components) {
        List<String> list = new ArrayList<>();
        for (Component component : components) {
            list.add(toLegacy(component));
        }
        return list;
    }

    public static List<String> toLegacy(Component... components) {
        List<String> list = new ArrayList<>();
        for (Component component : components) {
            list.add(toLegacy(component));
        }
        return list;
    }

    public static String replaceLegacy(String legacy) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            char current = legacy.charAt(i);
            char next = legacy.charAt(i == legacy.length() - 1 ? i : i + 1);
            if (current == '§' || current == '&') {
                if (next == 'x' && legacy.length() > i + 13) {
                    builder.append("<color:#");
                    builder.append(legacy.charAt(i + 3));
                    builder.append(legacy.charAt(i + 5));
                    builder.append(legacy.charAt(i + 7));

                    builder.append(legacy.charAt(i + 9));
                    builder.append(legacy.charAt(i + 11));
                    builder.append(legacy.charAt(i + 13));
                    builder.append(">");
                    i += 13;
                    continue;
                }
                String color = getColor(next);
                if (color == null) {
                    builder.append(current);
                    continue;
                }
                builder.append(color);
                i++;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String getColor(char c) {
        ChatColor color = ChatColor.getByChar(c);
        if (color == null) {
            return null;
        }

        switch (c) {
            case '0':
                return "<black>";
            case '1':
                return "<dark_blue>";
            case '2':
                return "<dark_green>";
            case '3':
                return "<dark_aqua>";
            case '4':
                return "<dark_red>";
            case '5':
                return "<dark_purple>";
            case '6':
                return "<gold>";
            case '7':
                return "<gray>";
            case '8':
                return "<dark_gray>";
            case '9':
                return "<blue>";
            case 'a':
                return "<green>";
            case 'b':
                return "<aqua>";
            case 'c':
                return "<red>";
            case 'd':
                return "<light_purple>";
            case 'e':
                return "<yellow>";
            case 'f':
                return "<white>";
            case 'k':
                return "<obfuscated>";
            case 'l':
                return "<b>";
            case 'm':
                return "<st>";
            case 'n':
                return "<u>";
            case 'o':
                return "<i>";
            case 'r':
                return "<reset>";
            default:
                return null;
        }
    }

    public static String clear(String msg) {
        msg = msg.replaceAll("&[0-9abcdefklmnor]", "");
        msg = msg.replaceAll("§[0-9abcdefklmnor]", "");
        msg = msg.replaceAll("&#[0-9a-fA-F]{6}", "");
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(msg));
    }

    public static String clear(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static List<Component> splitComponent(Component message, char c) {
        List<Component> components = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (char character : toLegacy(message).toCharArray()) {
            if (character == c) {
                components.add(formatComponent(builder.toString()));
                builder = new StringBuilder();
            } else {
                builder.append(character);
            }
        }
        components.add(formatComponent(builder.toString()));
        return components;
    }

    public static Component formatPlaceholder(Component message, MiniMessagePlaceholder... placeholder) {
        return message.replaceText(builder -> {
            for (MiniMessagePlaceholder place : placeholder) {
                builder.matchLiteral(place.getPlaceholder()).replacement(formatComponent(place.getValue()));
            }
        });
    }

    //Bukkit defaults for time
    public static Title createTitle(Component title, Component subtitle) {
        return Title.title(title, subtitle, Title.Times.times(
                Duration.of(10 * 50L, ChronoUnit.MILLIS),
                Duration.of(70 * 50L, ChronoUnit.MILLIS),
                Duration.of(20 * 50L, ChronoUnit.MILLIS)
        ));
    }
    // times in ticks
    public static Title createTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return Title.title(title, subtitle, Title.Times.times(
                Duration.of(fadeIn * 50L, ChronoUnit.MILLIS),
                Duration.of(stay * 50L, ChronoUnit.MILLIS),
                Duration.of(fadeOut * 50L, ChronoUnit.MILLIS)
        ));
    }

    public static Title createTitle(Component title, Component subtitle, Title.Times times) {
        return Title.title(title, subtitle, times);
    }

    public static void sendTitle(Title title, Player player) {
        SongodaCore.getSongodaPlugin().getBukkitAudiences().player(player).showTitle(title);
    }

    public static void sendActionBar(Component message, Player player) {
        SongodaCore.getSongodaPlugin().getBukkitAudiences().player(player).sendActionBar(message);
    }
}
