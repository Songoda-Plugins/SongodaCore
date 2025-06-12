package com.songoda.core.utils;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextUtilsTest {
    @Test
    void formatText() {
        final String testString = "hello&6 world &r:)";
        final String testStringResult = "hello" + ChatColor.COLOR_CHAR + "6 world " + ChatColor.COLOR_CHAR + "r:)";

        assertEquals("", TextUtils.formatText((String) null));
        assertEquals("", TextUtils.formatText(""));

        assertEquals(testStringResult, TextUtils.formatText(testString));

        assertEquals(Arrays.asList(testStringResult.split(" ")), TextUtils.formatText(testString.split(" ")));
        assertEquals(Arrays.asList(testStringResult.split(" ")), TextUtils.formatText(Arrays.asList(testString.split(" "))));
    }

    @Test
    void formatTextCapitalize() {
        assertEquals("Hello§6§l world §r:)", TextUtils.formatText("hello&6&l world &r:)", true));
//        assertEquals("§2Hello§6§l world §r:)", TextUtils.formatText("&2hello&6&l world &r:)", true)); // FIXME: https://github.com/songoda/SongodaCore/issues/26
    }

    @Test
    void wrap() {
        assertEquals(Arrays.asList("Lorem ipsum dolor sit amet,", " consectetuer adipiscing elit.",
                        " Aenean commodo ligula eget", " dolor. Aenean massa. Cum",
                        " sociis natoque penatibus", " et magnis dis parturient",
                        " montes, nascetur ridiculus", " mus."),
                TextUtils.wrap("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor." +
                        " Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus."));

        assertEquals(Collections.singletonList("§6Hello world!"), TextUtils.wrap("6", "Hello world!"));
    }

    @Test
    void convertToInvisibleLoreString() {
        assertEquals("", TextUtils.convertToInvisibleLoreString(null));
        assertEquals("", TextUtils.convertToInvisibleLoreString(""));

        assertEquals("§;§§§;§6§;§H§;§e§;§l§;§l§;§o§;§ §;§w§;§o§;§r§;§l§;§d", TextUtils.convertToInvisibleLoreString("§6Hello world"));
        assertEquals("§;§&§;§6§;§H§;§e§;§l§;§l§;§o§;§ §;§w§;§o§;§r§;§l§;§d", TextUtils.convertToInvisibleLoreString("&6Hello world"));
    }

    @Test
    void convertToInvisibleString() {
        assertEquals("", TextUtils.convertToInvisibleString(null));
        assertEquals("", TextUtils.convertToInvisibleString(""));

        assertEquals("§§§6§H§e§l§l§o§ §w§o§r§l§d", TextUtils.convertToInvisibleString("§6Hello world"));
        assertEquals("§&§6§H§e§l§l§o§;§w§o§r§l§d", TextUtils.convertToInvisibleString("&6Hello;world"));
    }

    @Test
    void convertFromInvisibleString() {
        assertEquals("", TextUtils.convertFromInvisibleString(null));
        assertEquals("", TextUtils.convertFromInvisibleString(""));

        assertEquals("§;§§§;§6§;§H§;§e§;§l§;§l§;§o§;§ §;§w§;§o§;§r§;§l§;§d", TextUtils.convertToInvisibleLoreString("§6Hello world"));
        assertEquals("6Helloworld", TextUtils.convertFromInvisibleString("§§§6§H§e§l§l§o§;§w§o§r§l§d"));
        assertEquals("&6Hello world", TextUtils.convertFromInvisibleString("§&§6§H§e§l§l§o§ §w§o§r§l§d"));
    }

    @Test
    void detectCharset() throws IOException {
        byte[] asciiString = "Hello world!\n".getBytes(StandardCharsets.US_ASCII);
        byte[] utf8String = "\uD83D\uDE43 Hello world!\n".getBytes(StandardCharsets.UTF_8);
        byte[] loremIpsum = ("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\n" +
                "\n" +
                "Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu.\n" +
                "\n" +
                "In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus.\n" +
                "\n" +
                "Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum.\n" +
                "\n" +
                "Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum.\n" +
                "\n" +
                "Nam quam nunc, blandit vel, luctus pulvinar, hendrerit id, lorem. Maecenas nec odio et ante tincidunt tempus. Donec vitae sapien ut libero venenatis faucibus. Nullam quis ante. Etiam sit amet orci eget eros faucibus tincidunt. Duis leo. Sed fringilla mauris sit amet nibh. Donec sodales sagittis magna. Sed consequat, leo eget bibendum sodales, augue velit cursus nunc,").getBytes(StandardCharsets.UTF_8);
        byte[] utf16LeBom = new byte[] {(byte) 0xFF, (byte) 0xFE, 'a', 'b', 'c'};
        byte[] utf16BeBom = new byte[] {(byte) 0xFE, (byte) 0xFF, 'a', 'b', 'c'};
        byte[] utf8Bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c'};

        assertEquals(StandardCharsets.UTF_8, TextUtils.detectCharset(asciiString, asciiString.length, null));
        assertEquals(StandardCharsets.UTF_8, TextUtils.detectCharset(utf8String, utf8String.length, null));

        assertEquals(StandardCharsets.UTF_16LE, TextUtils.detectCharset(utf16LeBom, utf16LeBom.length, null));
        assertEquals(StandardCharsets.UTF_16BE, TextUtils.detectCharset(utf16BeBom, utf16BeBom.length, null));
        assertEquals(StandardCharsets.UTF_8, TextUtils.detectCharset(utf8Bom, utf8Bom.length, null));

        Path tmpFilePath = Files.createTempFile(null, null);
        File tmpFile = tmpFilePath.toFile();
        tmpFile.deleteOnExit();

        try (OutputStream writer = new FileOutputStream(tmpFile)) {
            for (int i = 0; i < 5; ++i) {
                writer.write(loremIpsum);
            }

            writer.flush();
        }

        assertEquals(StandardCharsets.UTF_8, TextUtils.detectCharset(tmpFile, null));
        Files.deleteIfExists(tmpFilePath);

        assertNull(TextUtils.detectCharset(tmpFile, null));

        try (BufferedInputStream tmpIn = new BufferedInputStream(new ByteArrayInputStream(loremIpsum))) {
            assertEquals(StandardCharsets.UTF_8, TextUtils.detectCharset(tmpIn, null));
        }
    }
}
