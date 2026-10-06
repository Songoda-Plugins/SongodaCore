package com.songoda.core.item.resolver;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ItemResolverManagerTest {

    @Test
    public void testStripMinecraftPrefix() {
        assertEquals("diamond", ItemResolverManager.stripMinecraftPrefix("minecraft:diamond"));
        assertEquals("GOLD_INGOT", ItemResolverManager.stripMinecraftPrefix("MINECRAFT:GOLD_INGOT"));
        assertEquals("MiNeCrAfT:iron_axe", ItemResolverManager.stripMinecraftPrefix("minecraft:MiNeCrAfT:iron_axe"));
        assertEquals("nexo:sword", ItemResolverManager.stripMinecraftPrefix("nexo:sword"));
        assertEquals("stone", ItemResolverManager.stripMinecraftPrefix("stone"));
        assertNull(ItemResolverManager.stripMinecraftPrefix(null));
        assertEquals("", ItemResolverManager.stripMinecraftPrefix(""));
    }

    @Test
    public void testRegisterAndGetResolver() {
        CustomItemResolver resolver = new CustomItemResolver() {
            @Override
            public @NonNull String getPrefix() {
                return "testmock";
            }

            @Override
            public ItemStack resolve(@NonNull String itemId) {
                if ("sword".equalsIgnoreCase(itemId)) {
                    return new ItemStack(Material.DIAMOND_SWORD);
                }
                return null;
            }

            @Override
            public boolean isItem(@NonNull ItemStack itemStack, @NonNull String itemId) {
                return false;
            }

            @Override
            public String getItemId(@NonNull ItemStack itemStack) {
                return null;
            }
        };

        ItemResolverManager.registerResolver(resolver);
        try {
            CustomItemResolver retrieved = ItemResolverManager.getResolver("testmock");
            assertNotNull(retrieved);
            assertSame(resolver, retrieved);

            CustomItemResolver retrievedUpper = ItemResolverManager.getResolver("TESTMOCK");
            assertNotNull(retrievedUpper);
            assertSame(resolver, retrievedUpper);

            assertNull(ItemResolverManager.getResolver("non_existent_prefix"));
        } finally {
            ItemResolverManager.unregisterResolver(resolver);
        }

        assertNull(ItemResolverManager.getResolver("testmock"));
    }

    static class DummyItemStack extends ItemStack {
        private final Material type;

        public DummyItemStack(Material type) {
            super();
            this.type = type;
        }

        @Override
        public Material getType() {
            return type;
        }
    }

    @Test
    public void testResolveCustomItem() {
        CustomItemResolver resolver = new CustomItemResolver() {
            @Override
            public @NonNull String getPrefix() {
                return "customplugin";
            }

            @Override
            public ItemStack resolve(@NonNull String itemId) {
                if ("ruby_dagger".equalsIgnoreCase(itemId)) {
                    return new DummyItemStack(Material.NETHERITE_SWORD);
                }
                return null;
            }

            @Override
            public boolean isItem(@NonNull ItemStack itemStack, @NonNull String itemId) {
                return false;
            }

            @Override
            public String getItemId(@NonNull ItemStack itemStack) {
                return null;
            }
        };

        ItemResolverManager.registerResolver(resolver);
        try {
            ItemStack resolved = ItemResolverManager.resolve("customplugin:ruby_dagger");
            assertNotNull(resolved);
            assertEquals(Material.NETHERITE_SWORD, resolved.getType());

            ItemStack notFound = ItemResolverManager.resolve("customplugin:unknown_item");
            assertNull(notFound);
        } finally {
            ItemResolverManager.unregisterResolver(resolver);
        }
    }

    @Test
    public void testResolveMultiColonNamespace() {
        CustomItemResolver resolver = new CustomItemResolver() {
            @Override
            public @NonNull String getPrefix() {
                return "mmoitems_test";
            }

            @Override
            public ItemStack resolve(@NonNull String itemId) {
                if ("SWORD:KATANA".equalsIgnoreCase(itemId)) {
                    return new DummyItemStack(Material.IRON_SWORD);
                }
                return null;
            }

            @Override
            public boolean isItem(@NonNull ItemStack itemStack, @NonNull String itemId) {
                return false;
            }

            @Override
            public String getItemId(@NonNull ItemStack itemStack) {
                return null;
            }
        };

        ItemResolverManager.registerResolver(resolver);
        try {
            ItemStack resolved = ItemResolverManager.resolve("mmoitems_test:SWORD:KATANA");
            assertNotNull(resolved);
            assertEquals(Material.IRON_SWORD, resolved.getType());
        } finally {
            ItemResolverManager.unregisterResolver(resolver);
        }
    }

    @Test
    public void testResolveVanillaMaterials() {
        if (org.bukkit.Bukkit.getServer() == null) {
            // In headless unit tests without a Paper server, verify resolution runs without throwing
            ItemResolverManager.resolve("DIAMOND");
            ItemResolverManager.resolve("minecraft:diamond");
            return;
        }

        ItemStack diamond = ItemResolverManager.resolve("DIAMOND");
        assertNotNull(diamond);
        assertEquals(Material.DIAMOND, diamond.getType());

        ItemStack diamondWithNamespace = ItemResolverManager.resolve("minecraft:diamond");
        assertNotNull(diamondWithNamespace);
        assertEquals(Material.DIAMOND, diamondWithNamespace.getType());

        ItemStack ironIngot = ItemResolverManager.resolve("iron_ingot");
        assertNotNull(ironIngot);
        assertEquals(Material.IRON_INGOT, ironIngot.getType());
    }

    @Test
    public void testResolveNullOrBlankOrUnknown() {
        assertNull(ItemResolverManager.resolve(null));
        assertNull(ItemResolverManager.resolve(""));
        assertNull(ItemResolverManager.resolve("   "));
        assertNull(ItemResolverManager.resolve("this_material_does_not_exist_xyz_123"));
    }
}
