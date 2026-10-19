package com.songoda.core.command;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class CommandSuggesterTest {

    @Test
    public void filterOnlinePlayersUsesCaseInsensitiveCachedNames() {
        UUID firstPlayerId = UUID.randomUUID();
        UUID secondPlayerId = UUID.randomUUID();
        UUID thirdPlayerId = UUID.randomUUID();
        OnlinePlayerNameCache.cacheName(firstPlayerId, "Alex");
        OnlinePlayerNameCache.cacheName(secondPlayerId, "alexandra");
        OnlinePlayerNameCache.cacheName(thirdPlayerId, "Steve");

        try {
            CommandSuggester suggester = new CommandSuggester() {
            };

            assertEquals(List.of("Alex", "alexandra"), suggester.filterOnlinePlayers("AL"));
            assertEquals(List.of(), suggester.filterOnlinePlayers("not-online"));
        } finally {
            OnlinePlayerNameCache.removeName(firstPlayerId);
            OnlinePlayerNameCache.removeName(secondPlayerId);
            OnlinePlayerNameCache.removeName(thirdPlayerId);
        }
    }
}
