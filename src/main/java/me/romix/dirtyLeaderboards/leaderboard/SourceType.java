package me.romix.dirtyLeaderboards.leaderboard;

import java.util.Locale;

public enum SourceType {
    STATISTIC,
    PLACEHOLDER,
    CUSTOM;

    public static SourceType parse(String raw) {
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
