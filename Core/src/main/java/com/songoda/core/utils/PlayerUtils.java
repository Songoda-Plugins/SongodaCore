package com.songoda.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PlayerUtils {
    public static void sendMessages(Player player, String... messages) {
        for (String message : messages) {
            player.sendMessage(message);
        }
    }

    public static void sendMessages(Player player, List<String> messages) {
        sendMessages(player, messages.toArray(new String[0]));
    }

    /**
     * Get a list of all the players that this player can "see"
     *
     * @param sender       user to check against, or null for all players
     * @param startingWith optional query to test: only players whose game names
     *                     start with this
     *
     * @return list of player names that are "visible" to the player
     *
     * @deprecated This method has a strong use-case for Command related code and should be reimplemented there
     */
    @Deprecated
    public static List<String> getVisiblePlayerNames(CommandSender sender, String startingWith) {
        Player player = sender instanceof Player ? (Player) sender : null;
        final String startsWith = startingWith == null || startingWith.isEmpty() ? null : startingWith.toLowerCase();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != player)
                .filter(p -> startsWith == null || p.getName().toLowerCase().startsWith(startsWith))
                .filter(p -> player == null || (player.canSee(p) && p.getMetadata("vanished").isEmpty()))
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all the players that this player can "see"
     *
     * @param sender       user to check against, or null for all players
     * @param startingWith optional query to test: only players whose game names
     *                     start with this
     *
     * @return list of player names that are "visible" to the player
     *
     * @deprecated This method has a strong use-case for Command related code and should be reimplemented there
     */
    @Deprecated
    public static List<String> getVisiblePlayerDisplayNames(CommandSender sender, String startingWith) {
        Player player = sender instanceof Player ? (Player) sender : null;
        final String startsWith = startingWith == null || startingWith.isEmpty() ? null : startingWith.replaceAll("[^a-zA-Z]", "").toLowerCase();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != player)
                .filter(p -> startsWith == null || p.getDisplayName().replaceAll("[^a-zA-Z]", "").toLowerCase().startsWith(startsWith))
                .filter(p -> player == null || (player.canSee(p) && p.getMetadata("vanished").isEmpty()))
                .map(Player::getDisplayName)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all the players that this player can "see"
     *
     * @param sender       user to check against, or null for all players
     * @param startingWith optional query to test: only players whose game names
     *                     start with this
     *
     * @return list of players that are "visible" to the player
     *
     * @deprecated This method has a strong use-case for Command related code and should be reimplemented there
     */
    @Deprecated
    public static List<Player> getVisiblePlayers(CommandSender sender, String startingWith) {
        Player player = sender instanceof Player ? (Player) sender : null;
        final String startsWith = startingWith == null || startingWith.isEmpty() ? null : startingWith.toLowerCase();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != player)
                .filter(p -> startsWith == null || p.getName().toLowerCase().startsWith(startsWith))
                .filter(p -> player == null || (player.canSee(p) && p.getMetadata("vanished").isEmpty()))
                .map(Player.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all online player names that start with a string.
     *
     * @param us         Ourselves / who is requesting the list. Will not return this player.
     * @param startsWith All names returned must start with this input string
     *
     * @return List of matching player IGN
     *
     * @deprecated This method has a strong use-case for Command related code and should be reimplemented there
     */
    @Deprecated
    public static List<String> getAllPlayers(CommandSender us, String startsWith) {
        final String arg = startsWith.toLowerCase();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> us != p && p.getName().startsWith(arg))
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all online player names that start with a string.
     *
     * @param us         Ourselves / who is requesting the list. Will not return this player.
     * @param startsWith All names returned must start with this input string
     *
     * @return List of matching player display names
     *
     * @deprecated This method has a strong use-case for Command related code and should be reimplemented there
     */
    @Deprecated
    public static List<String> getAllPlayersDisplay(CommandSender us, String startsWith) {
        // FIXME: Why do we need that regex? It just breaks the startsWith check
        //        + the DisplayName comparison is not made lower case
        final String arg = startsWith.replaceAll("[^a-zA-Z]", "").toLowerCase();

        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> us != p && p.getDisplayName().replaceAll("[^a-zA-Z]", "").startsWith(arg))
                .map(Player::getDisplayName)
                .collect(Collectors.toList());
    }

    /**
     * Search for and grab the closest match for a provided player name. <br />
     * Also checks player display names if there is not an exact match.
     *
     * @param player player to search for
     *
     * @return Player that closest matches the input name, or null if none found
     *
     * @deprecated This method has a strong use-case for Command related code and should probably be reimplemented there
     */
    @Deprecated
    public static Player findPlayer(String player) {
        Player found = Bukkit.getServer().getPlayer(player);

        if (found == null) {
            final String searchName = player.toLowerCase();
            final String searchDisplayName = player.replaceAll("[^a-zA-Z]", "").toLowerCase();
            int d = 999;

            for (Player p2 : Bukkit.getOnlinePlayers()) {
                final String test;
                if (p2.getName().toLowerCase().startsWith(searchName)) {
                    int d2 = p2.getName().length() - searchName.length();

                    if (d2 < d) {
                        found = p2;
                        d = d2;
                    } else if (d2 == d) {
                        found = null;
                    }
                } else if ((test = p2.getDisplayName().replaceAll("[^a-zA-Z]", "")).toLowerCase().startsWith(searchDisplayName)) {
                    int d2 = test.length() - searchDisplayName.length();

                    if (d2 < d) {
                        found = p2;
                        d = d2;
                    } else if (d2 == d) {
                        found = null;
                    }
                }
            }
        }

        return found;
    }

    public static Player getRandomPlayer() {
        final Collection<? extends Player> all = Bukkit.getOnlinePlayers();
        if (all.isEmpty()) {
            return null;
        }

        final Iterator<? extends Player> alli = all.iterator();

        int pick = ThreadLocalRandom.current().nextInt(all.size());

        for (; pick > 0; --pick) {
            alli.next();
        }

        return alli.next();
    }

    public static void giveItem(Player player, ItemStack item) {
        if (player == null || !player.isOnline() || item == null) {
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }
    }

    public static void giveItem(Player player, ItemStack... items) {
        if (player == null || !player.isOnline() || items == null || items.length == 0) {
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items);
        if (!leftover.isEmpty()) {
            final World world = player.getWorld();
            final Location location = player.getLocation();

            leftover.values().forEach(it -> world.dropItemNaturally(location, it));
        }
    }

    public static void giveItem(Player player, Collection<ItemStack> items) {
        if (player == null || !player.isOnline() || items == null || items.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));

        if (!leftover.isEmpty()) {
            final World world = player.getWorld();
            final Location location = player.getLocation();
            leftover.values().forEach(it -> world.dropItemNaturally(location, it));
        }
    }

    /**
     * @deprecated The method should be moved to a more appropriate class
     */
    @Deprecated
    public static int getNumberFromPermission(Player player, String permission, int def) {
        final Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();

        boolean set = false;
        int highest = 0;

        for (PermissionAttachmentInfo info : permissions) {
            final String perm = info.getPermission();

            if (!perm.startsWith(permission)) {
                continue;
            }

            final int index = perm.lastIndexOf('.');

            if (index == -1 || index == perm.length()) {
                continue;
            }

            String numStr = perm.substring(perm.lastIndexOf('.') + 1);
            if (numStr.equals("*")) {
                return def;
            }

            final int number = Integer.parseInt(numStr);

            if (number >= highest) {
                highest = number;
                set = true;
            }
        }

        return set ? highest : def;
    }
}
