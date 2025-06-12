package com.songoda.core.utils;

import com.songoda.core.chat.ColorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ColorUtils {
    private static final Map<ColorCode, ColorSet<Integer, Integer, Integer>> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put(ColorCode.BLACK, new ColorSet<>(0, 0, 0));
        COLOR_MAP.put(ColorCode.DARK_BLUE, new ColorSet<>(0, 0, 170));
        COLOR_MAP.put(ColorCode.DARK_GREEN, new ColorSet<>(0, 170, 0));
        COLOR_MAP.put(ColorCode.DARK_AQUA, new ColorSet<>(0, 170, 170));
        COLOR_MAP.put(ColorCode.DARK_RED, new ColorSet<>(170, 0, 0));
        COLOR_MAP.put(ColorCode.DARK_PURPLE, new ColorSet<>(170, 0, 170));
        COLOR_MAP.put(ColorCode.GOLD, new ColorSet<>(255, 170, 0));
        COLOR_MAP.put(ColorCode.GRAY, new ColorSet<>(170, 170, 170));
        COLOR_MAP.put(ColorCode.DARK_GRAY, new ColorSet<>(85, 85, 85));
        COLOR_MAP.put(ColorCode.BLUE, new ColorSet<>(85, 85, 255));
        COLOR_MAP.put(ColorCode.GREEN, new ColorSet<>(85, 255, 85));
        COLOR_MAP.put(ColorCode.AQUA, new ColorSet<>(85, 255, 255));
        COLOR_MAP.put(ColorCode.RED, new ColorSet<>(255, 85, 85));
        COLOR_MAP.put(ColorCode.LIGHT_PURPLE, new ColorSet<>(255, 85, 255));
        COLOR_MAP.put(ColorCode.YELLOW, new ColorSet<>(255, 255, 85));
        COLOR_MAP.put(ColorCode.WHITE, new ColorSet<>(255, 255, 255));
    }

    private static class ColorSet<R, G, B> {
        R red;
        G green;
        B blue;

        ColorSet(R red, G green, B blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public R getRed() {
            return this.red;
        }

        public G getGreen() {
            return this.green;
        }

        public B getBlue() {
            return this.blue;
        }
    }

    public static ColorCode fromRGB(int r, int g, int b) {
        TreeMap<Integer, ColorCode> closest = new TreeMap<>();
        COLOR_MAP.forEach((color, set) -> {
            int red = Math.abs(r - set.getRed());
            int green = Math.abs(g - set.getGreen());
            int blue = Math.abs(b - set.getBlue());

            closest.put(red + green + blue, color);
        });

        return closest.firstEntry().getValue();
    }
}
