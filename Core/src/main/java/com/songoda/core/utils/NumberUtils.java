package com.songoda.core.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class NumberUtils {
    /**
     * @deprecated This method does not take the plugin's configured locale into account.
     *         Additionally, the currencySymbol's position is not configurable in this method.
     */
    @Deprecated
    public static String formatEconomy(char currencySymbol, double number) {
        return currencySymbol + formatNumber(number);
    }

    /**
     * @deprecated This method does not take the plugin's configured locale into account.
     */
    @Deprecated
    public static String formatNumber(double number) {
        DecimalFormat decimalFormatter = new DecimalFormat(number == Math.ceil(number) ? "#,###" : "#,###.00");

        // This is done to specifically prevent the NBSP character from printing in foreign languages.
        DecimalFormatSymbols symbols = decimalFormatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        decimalFormatter.setDecimalFormatSymbols(symbols);

        return decimalFormatter.format(number);
    }

    /**
     * @deprecated Should be re-implemented as {@code formatWithSiPrefix} and take the plugin's configured locale into account.
     */
    @Deprecated
    public static String formatWithSuffix(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        }

        int exp = (int) (Math.log(count) / Math.log(1000));

        return String.format("%.1f%c", count / Math.pow(1000, exp),
                "kMGTPE".charAt(exp - 1)).replace(".0", "");
    }

    public static boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException ignore) {
        }
        return false;
    }

    /**
     * @deprecated This allows floating point numbers and negative/positive numbers.
     *         Should be re-implemented as {@code isNumber} if needed.
     */
    @Deprecated
    public static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        return s.matches("[-+]?\\d*\\.?\\d+");
    }
}
