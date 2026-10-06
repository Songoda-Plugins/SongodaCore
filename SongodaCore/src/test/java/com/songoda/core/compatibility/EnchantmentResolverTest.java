package com.songoda.core.compatibility;

import org.bukkit.NamespacedKey;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class EnchantmentResolverTest {

    @Test
    public void testResolveNullAndBlank() {
        assertNull(EnchantmentResolver.resolve((String) null));
        assertNull(EnchantmentResolver.resolve(""));
        assertNull(EnchantmentResolver.resolve("   "));
        assertNull(EnchantmentResolver.resolve((NamespacedKey) null));
    }

    @Test
    public void testResolveWithoutServerEnvironmentDoesNotThrow() {
        // Without an active Bukkit/Paper server running, resolution returns null safely.
        assertNull(EnchantmentResolver.resolve("unknown_enchantment"));
        assertNull(EnchantmentResolver.resolve(new NamespacedKey("test", "dummy")));
    }
}
