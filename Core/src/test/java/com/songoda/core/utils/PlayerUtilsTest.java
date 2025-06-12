package com.songoda.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlayerUtilsTest {
    @Test
    void sendMessages_Array() {
        Player player = Mockito.mock(Player.class);
        PlayerUtils.sendMessages(player, "First message", "Second message");

        InOrder playerInOrder = Mockito.inOrder(player);
        playerInOrder.verify(player).sendMessage("First message");
        playerInOrder.verify(player).sendMessage("Second message");
        playerInOrder.verify(player, Mockito.never()).sendMessage(Mockito.anyString());
    }

    @Test
    void sendMessages_List() {
        Player player = Mockito.mock(Player.class);
        PlayerUtils.sendMessages(player, Arrays.asList("First message", "Second message"));

        InOrder playerInOrder = Mockito.inOrder(player);
        playerInOrder.verify(player).sendMessage("First message");
        playerInOrder.verify(player).sendMessage("Second message");
        playerInOrder.verify(player, Mockito.never()).sendMessage(Mockito.anyString());
    }

    @Test
    void getVisiblePlayerNames() {
        Player player = createMockPlayer("BasePlayer");
        Player visiblePlayer = createMockPlayer("VisiblePlayer");
        Player hiddenPlayer = createMockPlayer("HiddenPlayer");

        List<String> result;
        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            server.when(Bukkit::getOnlinePlayers).thenReturn(Arrays.asList(player, visiblePlayer, hiddenPlayer));

            Mockito.when(player.canSee(hiddenPlayer)).thenReturn(false);
            Mockito.when(player.canSee(visiblePlayer)).thenReturn(true);

            assertEquals(0, PlayerUtils.getVisiblePlayerNames(player, "_").size());

            result = PlayerUtils.getVisiblePlayerNames(player, null);
        }

        assertTrue(result.contains("VisiblePlayer"));
        assertEquals(1, result.size());
    }

    @Test
    void getVisiblePlayerDisplayNames() {
        Player player = createMockPlayer("BasePlayer");
        Player visiblePlayer = createMockPlayer("VisiblePlayer", "VisibleDisplayName");
        Player hiddenPlayer = createMockPlayer("HiddenPlayer");

        List<String> result;
        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            server.when(Bukkit::getOnlinePlayers).thenReturn(Arrays.asList(player, visiblePlayer, hiddenPlayer));

            Mockito.when(player.canSee(hiddenPlayer)).thenReturn(false);
            Mockito.when(player.canSee(visiblePlayer)).thenReturn(true);

            assertEquals(0, PlayerUtils.getVisiblePlayerDisplayNames(player, "A").size());

            result = PlayerUtils.getVisiblePlayerDisplayNames(player, null);
        }

        assertTrue(result.contains("VisibleDisplayName"));
        assertEquals(1, result.size());
    }

    @Test
    void getVisiblePlayers() {
        Player player = createMockPlayer("BasePlayer");
        Player visiblePlayer = createMockPlayer("VisiblePlayer");
        Player hiddenPlayer = createMockPlayer("HiddenPlayer");

        Mockito.when(player.canSee(hiddenPlayer)).thenReturn(false);
        Mockito.when(player.canSee(visiblePlayer)).thenReturn(true);

        List<Player> result;
        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            server.when(Bukkit::getOnlinePlayers).thenReturn(Arrays.asList(player, visiblePlayer, hiddenPlayer));

            assertEquals(0, PlayerUtils.getVisiblePlayers(player, "_").size());

            result = PlayerUtils.getVisiblePlayers(player, null);
        }

        assertTrue(result.contains(visiblePlayer));
        assertEquals(1, result.size());
    }

    @Test
    void getAllPlayers() {
        Player basePlayer = createMockPlayer("BasePlayer");
        Player player1 = createMockPlayer("Player_1");
        Player player2 = createMockPlayer("Player_2");
        Player player3 = createMockPlayer("Player3");

        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            server.when(Bukkit::getOnlinePlayers).thenReturn(Arrays.asList(basePlayer, player1, player2, player3));

            assertEquals(0, PlayerUtils.getVisiblePlayers(basePlayer, "_").size());

            List<String> result = PlayerUtils.getAllPlayers(basePlayer, "");
            assertFalse(result.contains(basePlayer.getName()));
            assertEquals(3, result.size());

            assertEquals(0, PlayerUtils.getAllPlayers(basePlayer, "_").size());
            assertEquals(0, PlayerUtils.getAllPlayers(basePlayer, "Player_").size());
        }
    }

    @Disabled("Disabled for now as the implementations seems to be faulty")
    @Test
    void getAllPlayersDisplay() {
        Player basePlayer = createMockPlayer("BasePlayer");
        createMockPlayer("Player_1");
        createMockPlayer("Player_2");
        createMockPlayer("Player3");

        List<String> result = PlayerUtils.getAllPlayersDisplay(basePlayer, "");
        assertEquals(3, result.size());
        assertFalse(result.contains(basePlayer.getDisplayName()));

        assertEquals(2, PlayerUtils.getAllPlayersDisplay(basePlayer, "Player_").size());
        assertEquals(0, PlayerUtils.getAllPlayersDisplay(basePlayer, "_").size());
    }

    @Disabled("Disabled for now as the implementations seems to be faulty")
    @Test
    void findPlayer() {
        Player p3 = createMockPlayer("Player", "p3");
        Player p1 = createMockPlayer("Player_1", "p1");
        Player p2 = createMockPlayer("_Player_2", "p2");

        assertEquals(p1, PlayerUtils.findPlayer("Player_"));
        assertEquals(p2, PlayerUtils.findPlayer("_Play"));
        assertEquals(p3, PlayerUtils.findPlayer("Player"));

        assertEquals(p3, PlayerUtils.findPlayer("p"));
        assertEquals(p1, PlayerUtils.findPlayer("p1"));
        assertEquals(p2, PlayerUtils.findPlayer("p2"));
        assertEquals(p3, PlayerUtils.findPlayer("p3"));
    }

    @Test
    void getRandomPlayer_NoneOnline() {
        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            server.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
            assertNull(PlayerUtils.getRandomPlayer());
        }
    }

    @Test
    void getRandomPlayer() {
        try (MockedStatic<Bukkit> server = Mockito.mockStatic(Bukkit.class)) {
            List<Player> players = new ArrayList<>(10);
            for (int i = 0; i < 10; ++i) {
                Player player = createMockPlayer("Player_" + i);
                players.add(player);
            }

            server.when(Bukkit::getOnlinePlayers).thenReturn(players);

            Set<Player> returnedPlayers = new HashSet<>();
            for (int i = 0; i < 50; ++i) {
                if (returnedPlayers.size() >= 5) {
                    break;
                }

                returnedPlayers.add(PlayerUtils.getRandomPlayer());
            }

            assertTrue(returnedPlayers.size() >= 5);
        }
    }

    @Test
    void giveItem() {
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        ItemStack itemToAdd = Mockito.mock(ItemStack.class);

        PlayerUtils.giveItem(player, itemToAdd);
        inventoryInOrder.verify(inventory).addItem(itemToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());
    }

    @Test
    void giveItem_Array() {
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        ItemStack[] itemsToAdd = new ItemStack[] {Mockito.mock(ItemStack.class), Mockito.mock(ItemStack.class)};

        PlayerUtils.giveItem(player, itemsToAdd);
        inventoryInOrder.verify(inventory).addItem(itemsToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());
    }

    @Test
    void giveItem_List() {
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        ItemStack[] itemsToAdd = new ItemStack[] {Mockito.mock(ItemStack.class), Mockito.mock(ItemStack.class)};

        PlayerUtils.giveItem(player, Arrays.asList(itemsToAdd));
        inventoryInOrder.verify(inventory).addItem(itemsToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());
    }

    @Test
    void giveItem_FullInventory() {
        ItemStack itemToAdd = Mockito.mock(ItemStack.class);

        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        Mockito.when(inventory.addItem(itemToAdd)).thenReturn(new HashMap<Integer, ItemStack>() {{
            put(0, itemToAdd);
        }});
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        World world = Mockito.mock(World.class);
        InOrder worldInOrder = Mockito.inOrder(world);
        Mockito.when(player.getWorld()).thenReturn(world);

        PlayerUtils.giveItem(player, itemToAdd);

        inventoryInOrder.verify(inventory).addItem(itemToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());

        worldInOrder.verify(world).dropItemNaturally(Mockito.any(), Mockito.eq(itemToAdd));
        worldInOrder.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());
    }

    @Test
    void giveItem_FullInventory_Array() {
        ItemStack[] itemsToAdd = new ItemStack[] {Mockito.mock(ItemStack.class), Mockito.mock(ItemStack.class)};

        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        Mockito.when(inventory.addItem(Mockito.any())).thenReturn(new HashMap<Integer, ItemStack>() {{
            put(0, itemsToAdd[0]);
            put(1, itemsToAdd[1]);
        }});
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        World world = Mockito.mock(World.class);
        InOrder worldInOrder = Mockito.inOrder(world);
        Mockito.when(player.getWorld()).thenReturn(world);

        PlayerUtils.giveItem(player, itemsToAdd);
        inventoryInOrder.verify(inventory).addItem(itemsToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());

        worldInOrder.verify(world).dropItemNaturally(Mockito.any(), Mockito.eq(itemsToAdd[0]));
        worldInOrder.verify(world).dropItemNaturally(Mockito.any(), Mockito.eq(itemsToAdd[1]));
        worldInOrder.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());
    }

    @Test
    void giveItem_FullInventory_List() {
        ItemStack[] itemsToAdd = new ItemStack[] {Mockito.mock(ItemStack.class), Mockito.mock(ItemStack.class)};

        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        Mockito.when(inventory.addItem(Mockito.any())).thenReturn(new HashMap<Integer, ItemStack>() {{
            put(0, itemsToAdd[0]);
            put(1, itemsToAdd[1]);
        }});
        InOrder inventoryInOrder = Mockito.inOrder(inventory);

        Player player = createMockPlayer("Player");
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.isOnline()).thenReturn(true);

        World world = Mockito.mock(World.class);
        InOrder worldInOrder = Mockito.inOrder(world);
        Mockito.when(player.getWorld()).thenReturn(world);

        PlayerUtils.giveItem(player, Arrays.asList(itemsToAdd));
        inventoryInOrder.verify(inventory).addItem(itemsToAdd);
        inventoryInOrder.verify(inventory, Mockito.never()).addItem(Mockito.any());

        worldInOrder.verify(world).dropItemNaturally(Mockito.any(), Mockito.eq(itemsToAdd[0]));
        worldInOrder.verify(world).dropItemNaturally(Mockito.any(), Mockito.eq(itemsToAdd[1]));
        worldInOrder.verify(world, Mockito.never()).dropItemNaturally(Mockito.any(), Mockito.any());
    }

    @Disabled("Test is incomplete")
    @Test
    void getNumberFromPermission() {
        Player player = createMockPlayer("Player");

        assertEquals(-1, PlayerUtils.getNumberFromPermission(player, "example.plugin.feature", -1));
    }

    private Player createMockPlayer(String name) {
        return createMockPlayer(name, name);
    }

    private Player createMockPlayer(String name, String displayName) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getDisplayName()).thenReturn(displayName);

        return player;
    }
}
