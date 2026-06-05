package com.songoda.core.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {
    @Test
    void makeReadable() {
        assertEquals("", TimeUtils.makeReadable(null));

        assertEquals("20m", TimeUtils.makeReadable(TimeUnit.MINUTES.toMillis(20)));
        assertEquals("20m 10s", TimeUtils.makeReadable(
                TimeUnit.MINUTES.toMillis(20) + TimeUnit.SECONDS.toMillis(10)));

        assertEquals("10d 2h 32m 12s", TimeUtils.makeReadable(
                TimeUnit.DAYS.toMillis(10) +
                        TimeUnit.HOURS.toMillis(2) +
                        TimeUnit.MINUTES.toMillis(32) +
                        TimeUnit.SECONDS.toMillis(12) +
                        100
        ));
    }

    @Test
    void parseTime() {
        assertEquals(TimeUnit.MINUTES.toMillis(20), TimeUtils.parseTime("20m"));
        assertEquals(TimeUnit.MINUTES.toMillis(20) + TimeUnit.SECONDS.toMillis(10), TimeUtils.parseTime("20m 10s"));

        assertEquals(TimeUnit.DAYS.toMillis(10) +
                        TimeUnit.HOURS.toMillis(2) +
                        TimeUnit.MINUTES.toMillis(32) +
                        TimeUnit.SECONDS.toMillis(12),
                TimeUtils.parseTime("10d 2h 32m 12s"));

        assertEquals(TimeUnit.MINUTES.toMillis(100) +
                        TimeUnit.SECONDS.toMillis(100),
                TimeUtils.parseTime("100m 100s"));
        assertEquals(TimeUnit.MINUTES.toMillis(100) +
                        TimeUnit.SECONDS.toMillis(100),
                TimeUtils.parseTime("100m100s"));

        assertEquals(0, TimeUtils.parseTime("10?"));
        assertEquals(0, TimeUtils.parseTime("10xX"));
    }
}
