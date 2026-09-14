package me.romix.dirtyLeaderboards.config;

import java.util.List;

public record DisplayStyle(
        String nameColor,
        String onlineNameFormat,
        String dotsColor,
        List<String> rankColors,
        String defaultRankColor,
        int maxPixelWidth,
        int maxNamePixelWidth,
        int maxTitlePixelWidth,
        int headSpriteWidth,
        int maxMiddleDots,
        double titleScaleMultiplier,
        double rowScaleMultiplier,
        double marginPercent,
        double titleYRatio,
        double progressBarYRatio,
        double rowsTopYRatio,
        double rowSpacingRatio
) {

    public String rankColor(int rank) {
        if (rank >= 1 && rank <= rankColors.size()) {
            return rankColors.get(rank - 1);
        }
        return defaultRankColor;
    }
}
