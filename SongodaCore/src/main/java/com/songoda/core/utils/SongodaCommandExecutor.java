package com.songoda.core.utils;

import com.songoda.core.command.CommandSuggester;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public interface SongodaCommandExecutor extends TabExecutor {


    default List<String> filterCompletions(String arg, String... completions) {
        return Stream.of(completions)
                .filter(completion -> completion.toLowerCase(Locale.ENGLISH).startsWith(arg.toLowerCase()))
                .toList();
    }

    default List<String> filterOnlinePlayers(String arg) {
        return CommandSuggester.filterOnlinePlayerNames(arg);
    }
}
