package com.songoda.core.utils;

import com.songoda.core.text.lang.Lang;

public class TimeUtils {

    /**
     * Converts a time string to seconds.
     * Example imput: 1y 1mo 1w 1d 1h 1m 1s (year, month, week, day, hour, minute, second)
     *
     * @param time The time string to convert.
     * @return The time in seconds.
     */
    public static long convertToSeconds(String time) {
        if (time == null || time.isEmpty()) return 0;

        long seconds = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(y|mo|w|d|h|m|s)");
        java.util.regex.Matcher matcher = pattern.matcher(time);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "y":
                    seconds += value * 31536000L; // 365 days
                    break;
                case "mo":
                    seconds += value * 2592000L; // 30 days
                    break;
                case "w":
                    seconds += value * 604800L; // 7 days
                    break;
                case "d":
                    seconds += value * 86400L;
                    break;
                case "h":
                    seconds += value * 3600L;
                    break;
                case "m":
                    seconds += value * 60L;
                    break;
                case "s":
                    seconds += value;
                    break;
            }
        }
        return seconds;
    }

    /**
     * Converts seconds to a human-readable time string.
     *
     * @param seconds The time in seconds to convert.
     * @return The human-readable time string.
     */
    public static String secondsToTime(long seconds) {
        StringBuilder time = new StringBuilder();

        long years = seconds / 31536000;
        seconds %= 31536000;

        long months = seconds / 2592000;
        seconds %= 2592000;

        long weeks = seconds / 604800;
        seconds %= 604800;

        long days = seconds / 86400;
        seconds %= 86400;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        if (years > 0) time.append(years).append(Lang.getString("Time Units.Year", "y")).append(" ");
        if (months > 0) time.append(months).append(Lang.getString("Time Units.Month", "mo")).append(" ");
        if (weeks > 0) time.append(weeks).append(Lang.getString("Time Units.Week", "w")).append(" ");
        if (days > 0) time.append(days).append(Lang.getString("Time Units.Day", "d")).append(" ");
        if (hours > 0) time.append(hours).append(Lang.getString("Time Units.Hour", "h")).append(" ");
        if (minutes > 0) time.append(minutes).append(Lang.getString("Time Units.Minute", "m")).append(" ");
        if (seconds > 0) time.append(seconds).append(Lang.getString("Time Units.Second", "s")).append(" ");

        return time.toString().trim();
    }
}
