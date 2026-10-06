package com.songoda.core.test;

public class Main {

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
        return switch (c) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<b>";
            case 'm' -> "<st>";
            case 'n' -> "<u>";
            case 'o' -> "<i>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    public static void main(String[] args) {
        String test = "<primary-color>§7Cobblestone</color> <secondary-color>1</color>";
        System.out.println(replaceLegacy(test));
    }
}
