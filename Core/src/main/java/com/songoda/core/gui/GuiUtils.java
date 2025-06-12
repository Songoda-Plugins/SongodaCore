package com.songoda.core.gui;

import com.songoda.core.chat.AdventureUtils;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.locale.Message;
import com.cryptomorin.xseries.XMaterial;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuiUtils {
    public static ItemStack getBorderGlassItem() {
        ItemStack glass = XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem();
        ItemMeta glassmeta = glass.getItemMeta();

        glassmeta.setDisplayName(ChatColor.BLACK.toString());
        glass.setItemMeta(glassmeta);

        return glass;
    }

    public static ItemStack getBorderItem(ItemStack item) {
        ItemMeta glassmeta = item.getItemMeta();

        glassmeta.setDisplayName(ChatColor.BLACK.toString());
        item.setItemMeta(glassmeta);

        return item;
    }

    public static ItemStack getBorderItem(XMaterial mat) {
        ItemStack item = mat.parseItem();
        ItemMeta glassmeta = item.getItemMeta();

        glassmeta.setDisplayName(ChatColor.BLACK.toString());
        item.setItemMeta(glassmeta);

        return item;
    }

    public static List<String> getSafeLore(String... lines) {
        return getSafeLore(Arrays.asList(lines));
    }

    /**
     * Get a lore value that will display fine on clients using auto gui scaling
     *
     * @param lines lines to format
     *
     * @return newline and length-corrected item lore
     */
    public static List<String> getSafeLore(List<String> lines) {
        // fix newlines
        ArrayList<String> newLore = new ArrayList<>();

        for (String line : lines) {
            for (String l2 : line.split("\n")) {
                if (l2.length() < 54) {
                    newLore.add(l2);
                    continue;
                }

                // try to shorten the string
                String shorterString = l2;
                ChatColor lastColor = null; // todo? probably should also track formatting codes..
                int lineNumber = 0;
                while (shorterString.length() > 50) {
                    int breakingSpace = -1;

                    for (int i = 0; i < 50; ++i) {
                        if (shorterString.charAt(i) == ChatColor.COLOR_CHAR) {
                            lastColor = ChatColor.getByChar(shorterString.charAt(++i));
                        } else if (shorterString.charAt(i) == ' ' || shorterString.charAt(i) == '-') {
                            breakingSpace = i;
                        }
                    }

                    if (breakingSpace == -1) {
                        breakingSpace = Math.max(50, shorterString.length());
                        newLore.add((lineNumber != 0 && lastColor != null ? lastColor.toString() : "") + shorterString.substring(0, breakingSpace) + "-");
                        shorterString = breakingSpace == shorterString.length() ? "" : shorterString.substring(breakingSpace + 1);
                    } else {
                        newLore.add((lineNumber != 0 && lastColor != null ? lastColor.toString() : "") + shorterString.substring(0, breakingSpace));
                        shorterString = breakingSpace == shorterString.length() ? "" : shorterString.substring(breakingSpace + 1);
                    }

                    ++lineNumber;
                }

                if (!shorterString.isEmpty()) {
                    newLore.add((lineNumber != 0 && lastColor != null ? lastColor.toString() : "") + "   " + shorterString);
                }
            }
        }

        return newLore;
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, String title, String... lore) {
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, Component title, Component... lore) {
        return createButtonItem(mat, 1, title, lore);
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, int amount, String title, String... lore) {
        ItemStack item = mat.parseItem();
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, int amount, Component title, Component... lore) {
        ItemStack item = mat.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Invalid material: " + mat);
        }
        item.setAmount(amount);
        AdventureUtils.formatItemName(item, title);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(ItemStack from, String title, String... lore) {
        ItemStack item = from.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, String title, List<String> lore) {
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, Component title, List<Component> lore) {
        return createButtonItem(mat, 1, title, lore);
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, int amount, String title, List<String> lore) {
        ItemStack item = mat.parseItem();
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, int amount, Component title, List<Component> lore) {
        ItemStack item = mat.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Invalid material: " + mat);
        }
        item.setAmount(amount);
        AdventureUtils.formatItemName(item, title);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(ItemStack from, String title, List<String> lore) {
        ItemStack item = from.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(ItemStack from, Component title, List<Component> lore) {
        ItemStack item = from.clone();
        AdventureUtils.formatItemName(item, title);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, String[] lore) {
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && lore.length != 0) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, int amount, String[] lore) {
        ItemStack item = mat.parseItem();
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && lore.length != 0) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, int amount, Component... lore) {
        ItemStack item = mat.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Invalid material: " + mat);
        }
        item.setAmount(amount);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(ItemStack from, String[] lore) {
        ItemStack item = from.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && lore.length != 0) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(ItemStack from, Component... lore) {
        ItemStack item = from.clone();
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, List<String> lore, String... unused) {
        ItemStack item = mat.parseItem();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && !lore.isEmpty()) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, List<Component> lore) {
        return createButtonItem(mat, 1, lore);
    }

    @Deprecated
    public static ItemStack createButtonItem(XMaterial mat, int amount, List<String> lore, String... unused) {
        ItemStack item = mat.parseItem();
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && !lore.isEmpty()) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial mat, int amount, List<Component> lore) {
        ItemStack item = mat.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Invalid material: " + mat);
        }
        item.setAmount(amount);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack createButtonItem(ItemStack from, List<String> lore, String... unused) {
        ItemStack item = from.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && !lore.isEmpty()) {
                List<String> safe = getSafeLore(lore);

                meta.setDisplayName(safe.get(0));
                meta.setLore(safe.subList(1, safe.size()));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createButtonItem(ItemStack from, List<Component> lore) {
        ItemStack item = from.clone();
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, String title, String... lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, Component title, Component... lore) {
        AdventureUtils.formatItemName(item, title);
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack updateItemName(ItemStack item, String title) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItemName(ItemStack item, Component title) {
        AdventureUtils.formatItemName(item, title);

        return item;
    }

    @Deprecated
    public static ItemStack updateItemLore(ItemStack item, String... lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null && lore.length != 0) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItemLore(ItemStack item, Component... lore) {
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack updateItemLore(ItemStack item, List<String> lore, String... unused) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItemLore(ItemStack item, List<Component> lore) {
        AdventureUtils.formatItemLore(item, lore);

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, XMaterial matTo, String title, String... lore) {
        if (!matTo.isSimilar(item)) {
            item = matTo.parseItem();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, XMaterial matTo, Component title, Component... lore) {
        if (!matTo.isSimilar(item)) {
            item = matTo.parseItem();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            AdventureUtils.formatItemName(item, title);
            AdventureUtils.formatItemLore(item, lore);
        }

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, ItemStack to, String title, String... lore) {
        if (!CompatibleMaterial.getMaterial(item.getType()).get().isSimilar(to)) {
            item = to.clone();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, ItemStack to, Component title, Component... lore) {
        if (!CompatibleMaterial.getMaterial(item.getType()).get().isSimilar(to)) {
            item = to.clone();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            AdventureUtils.formatItemName(item, title);
            AdventureUtils.formatItemLore(item, lore);
        }

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, String title, List<String> lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, Component title, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            AdventureUtils.formatItemName(item, title);
            AdventureUtils.formatItemLore(item, lore);
        }

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, XMaterial matTo, String title, List<String> lore) {
        if (!matTo.isSimilar(item)) {
            item = matTo.parseItem();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, XMaterial matTo, Component title, List<Component> lore) {
        if (!matTo.isSimilar(item)) {
            item = matTo.parseItem();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            AdventureUtils.formatItemName(item, title);
            AdventureUtils.formatItemLore(item, lore);
        }

        return item;
    }

    @Deprecated
    public static ItemStack updateItem(ItemStack item, ItemStack to, String title, List<String> lore) {
        if (!CompatibleMaterial.getMaterial(item.getType()).get().isSimilar(to)) {
            item = to.clone();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title);

            if (lore != null) {
                meta.setLore(getSafeLore(lore));
            } else {
                meta.setLore(Collections.emptyList());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack updateItem(ItemStack item, ItemStack to, Component title, List<Component> lore) {
        if (!CompatibleMaterial.getMaterial(item.getType()).get().isSimilar(to)) {
            item = to.clone();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            AdventureUtils.formatItemName(item, title);
            AdventureUtils.formatItemLore(item, lore);
        }

        return item;
    }

    public static ItemStack createButtonItem(XMaterial material, Message title) {
        return createButtonItem(material, title.getMessage());
    }

    public static ItemStack createButtonItem(XMaterial material, Message title, Message lore) {
        return createButtonItem(material, title.getMessage(), lore.getMessage());
    }
}
