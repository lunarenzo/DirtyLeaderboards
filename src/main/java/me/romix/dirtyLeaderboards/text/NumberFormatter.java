package me.romix.dirtyLeaderboards.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public final class NumberFormatter {

    private final DecimalFormat grouped;
    private final long compactFrom;
    private final List<String> suffixes;

    public NumberFormatter(String groupingSeparator, long compactFrom, List<String> suffixes) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        boolean grouping = !groupingSeparator.isEmpty();
        if (grouping) {
            symbols.setGroupingSeparator(groupingSeparator.charAt(0));
        }
        this.grouped = new DecimalFormat(grouping ? "#,##0.##" : "0.##", symbols);
        this.grouped.setRoundingMode(RoundingMode.DOWN);
        this.compactFrom = compactFrom;
        this.suffixes = List.copyOf(suffixes);
    }

    public String format(double value) {
        if (compactFrom > 0 && !suffixes.isEmpty() && Math.abs(value) >= compactFrom) {
            return compact(value);
        }
        return grouped.format(value);
    }

    private String compact(double value) {
        double scaled = value;
        String suffix = "";
        for (String candidate : suffixes) {
            if (Math.abs(scaled) < 1000) {
                break;
            }
            scaled /= 1000;
            suffix = candidate;
        }
        double magnitude = Math.abs(scaled);
        int decimals = magnitude < 10 ? 2 : magnitude < 100 ? 1 : 0;
        return BigDecimal.valueOf(scaled)
                .setScale(decimals, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString() + suffix;
    }
}
