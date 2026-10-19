package com.songoda.core.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public interface CommandSuggester {

    default List<String> filterCompletions(String arg, String... completions) {
        return Stream.of(completions)
                .filter(completion -> completion.toLowerCase(Locale.ENGLISH).startsWith(arg.toLowerCase()))
                .toList();
    }

    default List<String> filterOnlinePlayers(String arg) {
        return filterOnlinePlayerNames(arg);
    }

    static List<String> filterOnlinePlayerNames(String partial) {
        return OnlinePlayerNameCache.findNamesStartingWith(partial);
    }
}
