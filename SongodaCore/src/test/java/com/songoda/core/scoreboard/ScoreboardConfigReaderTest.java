package com.songoda.core.scoreboard;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ScoreboardConfigReaderTest {

    @Test
    public void readsNamedAnimationsAndLineDefinitions() {
        Map<String, Object> animation = new LinkedHashMap<>();
        animation.put("Frames", List.of("A", "AB"));
        animation.put("Interval", 5);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("Text", "Balance: %vault_eco_balance%");
        line.put("Update", 10);

        Map<String, Object> spawn = new LinkedHashMap<>();
        spawn.put("Title", "Welcome %player_name%");
        spawn.put("Lines", List.of(line, Map.of("Animation", "logo")));

        ScoreboardConfiguration configuration = ScoreboardConfigReader.read(Map.of(
                "Placeholder Updates", List.of("%player_name%:0", "%vault_eco_balance%:100"),
                "Animations", Map.of("logo", animation),
                "Scoreboards", Map.of("spawn", spawn)
        ));

        assertEquals(Long.valueOf(0L), configuration.placeholderUpdates().get("%player_name%"));
        assertEquals(Long.valueOf(100L), configuration.placeholderUpdates().get("%vault_eco_balance%"));
        assertEquals(List.of("A", "AB"), configuration.animations().get("logo").frames());
        assertEquals(5L, configuration.animations().get("logo").updateTicks());
        assertEquals("Balance: %vault_eco_balance%", configuration.scoreboards()
                .get("spawn").lines().get(0).text());
        assertEquals("logo", configuration.scoreboards().get("spawn").lines().get(1).animation());
    }
}
