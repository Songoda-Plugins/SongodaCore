package com.songoda.core.hooks.plugin.placeholderapi;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConditionParserTest {

    @Test
    public void testBasicNumericComparisons() {
        assertTrue(ConditionParser.evaluate("5 > 3", null));
        assertTrue(ConditionParser.evaluate("10 >= 10", null));
        assertTrue(ConditionParser.evaluate("2 < 4", null));
        assertTrue(ConditionParser.evaluate("5 <= 5", null));
        assertTrue(ConditionParser.evaluate("10 == 10", null));
        assertTrue(ConditionParser.evaluate("10 != 5", null));

        assertFalse(ConditionParser.evaluate("5 < 3", null));
        assertFalse(ConditionParser.evaluate("5 == 10", null));
        assertFalse(ConditionParser.evaluate("5 != 5", null));
    }

    @Test
    public void testNegativeNumbers() {
        assertTrue(ConditionParser.evaluate("-5 < 0", null));
        assertTrue(ConditionParser.evaluate("-10 <= -10", null));
        assertTrue(ConditionParser.evaluate("5 > -5", null));
        assertFalse(ConditionParser.evaluate("-5 > 0", null));
    }

    @Test
    public void testLogicalOperators() {
        assertTrue(ConditionParser.evaluate("5 > 3 && 10 > 5", null));
        assertTrue(ConditionParser.evaluate("5 > 3 || 10 < 5", null));
        assertTrue(ConditionParser.evaluate("5 < 3 || 10 > 5", null));
        assertFalse(ConditionParser.evaluate("5 < 3 && 10 > 5", null));
        assertFalse(ConditionParser.evaluate("5 < 3 || 10 < 5", null));
    }

    @Test
    public void testStringComparisons() {
        assertTrue(ConditionParser.evaluate("'VIP' == 'VIP'", null));
        assertTrue(ConditionParser.evaluate("'admin' == 'ADMIN'", null));
        assertTrue(ConditionParser.evaluate("'VIP' != 'DEFAULT'", null));
        assertTrue(ConditionParser.evaluate("\"hello world\" == \"hello world\"", null));
        assertFalse(ConditionParser.evaluate("'VIP' == 'DEFAULT'", null));
    }

    @Test
    public void testParentheses() {
        assertTrue(ConditionParser.evaluate("(5 > 3) && (2 < 4)", null));
        assertTrue(ConditionParser.evaluate("!(5 < 3)", null));
        assertTrue(ConditionParser.evaluate("((10 > 5) || (2 > 10)) && (3 == 3)", null));
    }
}
