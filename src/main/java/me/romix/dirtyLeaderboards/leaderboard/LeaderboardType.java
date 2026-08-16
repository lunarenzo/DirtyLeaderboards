package me.romix.dirtyLeaderboards.leaderboard;

import org.bukkit.Statistic;

public record LeaderboardType(
        String id,
        SourceType source,
        Statistic statistic,
        String statisticJsonKey,
        String placeholder,
        ValueFormat format,
        SortOrder sortOrder,
        double minimumValue,
        String title,
        String titleColor,
        String valueColor,
        String iconTemplate,
        int iconFrames,
        long iconFramePeriod,
        int iconFramePad,
        int iconWidth,
        int durationTicks,
        double progressBarLength,
        double progressBarWidth
) {

    private static final String FRAME_TOKEN = "%frame%";

    public String icon(double value) {
        if (iconFrames <= 1 || !iconTemplate.contains(FRAME_TOKEN)) {
            return iconTemplate;
        }
        long period = Math.max(1, iconFramePeriod);
        long wrapped = (((long) value) % period + period) % period;
        int frame = (int) Math.floor(wrapped / (double) period * iconFrames) + 1;
        String number = String.valueOf(Math.min(frame, iconFrames));
        StringBuilder padded = new StringBuilder(number);
        while (padded.length() < iconFramePad) {
            padded.insert(0, '0');
        }
        return iconTemplate.replace(FRAME_TOKEN, padded.toString());
    }

    public String titleIcon() {
        return icon(0);
    }
}
