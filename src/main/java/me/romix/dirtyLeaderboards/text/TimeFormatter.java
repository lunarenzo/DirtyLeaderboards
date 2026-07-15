package me.romix.dirtyLeaderboards.text;

import java.util.ArrayList;
import java.util.List;

public final class TimeFormatter {

    public record Unit(long seconds, String suffix) {
    }

    private final List<Unit> units;
    private final String separator;

    public TimeFormatter(List<Unit> units, String separator) {
        List<Unit> sorted = new ArrayList<>(units);
        sorted.sort((a, b) -> Long.compare(b.seconds(), a.seconds()));
        this.units = List.copyOf(sorted);
        this.separator = separator;
    }

    public String formatTicks(long ticks) {
        return formatSeconds(ticks / 20);
    }

    public String formatSeconds(long totalSeconds) {
        long value = Math.max(0, totalSeconds);
        if (units.isEmpty()) {
            return value + "s";
        }
        int primary = -1;
        for (int i = 0; i < units.size(); i++) {
            if (value >= units.get(i).seconds()) {
                primary = i;
                break;
            }
        }
        if (primary < 0) {
            Unit smallest = units.get(units.size() - 1);
            return "0" + smallest.suffix();
        }
        if (primary == units.size() - 1 && primary > 0) {
            Unit above = units.get(primary - 1);
            Unit unit = units.get(primary);
            return "0" + above.suffix() + separator + (value / unit.seconds()) + unit.suffix();
        }
        Unit unit = units.get(primary);
        long count = value / unit.seconds();
        long remainder = value % unit.seconds();
        String result = count + unit.suffix();
        if (primary + 1 < units.size()) {
            Unit next = units.get(primary + 1);
            long nextCount = remainder / next.seconds();
            if (nextCount > 0) {
                result += separator + nextCount + next.suffix();
            }
        }
        return result;
    }
}
