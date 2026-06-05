package com.songoda.core.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberUtilsTest {
    @Test
    void formatEconomy() {
        assertEquals("$1,999.99", NumberUtils.formatEconomy('$', 1999.99));
    }

    @Test
    void formatNumber() {
        assertEquals("1,999.99", NumberUtils.formatNumber(1999.99));
        assertEquals("1,999,999.99", NumberUtils.formatNumber(1999999.99));
    }

    @Disabled("Tested method's output relies on the executing system (Locale)")
    @Test
    void formatWithSuffix() {
        assertEquals("100", NumberUtils.formatWithSuffix(100));

        assertEquals("150,5M", NumberUtils.formatWithSuffix(150_500_000));
        assertEquals("150,5M", NumberUtils.formatWithSuffix(150_500_999));
        assertEquals("151,0M", NumberUtils.formatWithSuffix(150_999_999));

        assertEquals("150,5G", NumberUtils.formatWithSuffix(150_500_000_000L));
        assertEquals("150,5T", NumberUtils.formatWithSuffix(150_500_000_000_000L));
        assertEquals("150,5P", NumberUtils.formatWithSuffix(150_500_000_000_000_000L));
        assertEquals("1,5E", NumberUtils.formatWithSuffix(150_500_000_000_000_000_0L));

        assertEquals("9,2E", NumberUtils.formatWithSuffix(Long.MAX_VALUE));

        assertEquals(String.valueOf(Long.MIN_VALUE), NumberUtils.formatWithSuffix(Long.MIN_VALUE));
    }

    @Test
    void isInt() {
        assertFalse(NumberUtils.isInt(null));
        assertFalse(NumberUtils.isInt(""));

        assertTrue(NumberUtils.isInt("100"));
        assertTrue(NumberUtils.isInt("-100"));
        assertTrue(NumberUtils.isInt("+100"));

        assertFalse(NumberUtils.isInt("100.0"));
        assertFalse(NumberUtils.isInt("Int"));
    }

    @Test
    void isNumeric() {
        assertFalse(NumberUtils.isNumeric(null));
        assertFalse(NumberUtils.isNumeric(""));

        assertTrue(NumberUtils.isNumeric("100"));
        assertTrue(NumberUtils.isNumeric("100.0"));
        assertTrue(NumberUtils.isNumeric("-100.0"));
        assertTrue(NumberUtils.isNumeric("+100.0"));

        assertFalse(NumberUtils.isNumeric("Numeric"));
    }
}
