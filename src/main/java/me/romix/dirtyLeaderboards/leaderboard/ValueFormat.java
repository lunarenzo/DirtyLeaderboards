package me.romix.dirtyLeaderboards.leaderboard;

import java.util.Locale;

public enum ValueFormat {
    NUMBER,
    TIME;

    public static ValueFormat parse(String raw) {
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
