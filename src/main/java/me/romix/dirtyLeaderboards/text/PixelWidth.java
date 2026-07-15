package me.romix.dirtyLeaderboards.text;

import org.bukkit.map.MinecraftFont;

public final class PixelWidth {

    private static final MinecraftFont FONT = MinecraftFont.Font;

    private PixelWidth() {
    }

    public static int of(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        if (FONT.isValid(text)) {
            return FONT.getWidth(text);
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            clean.append(FONT.isValid(String.valueOf(c)) ? c : '?');
        }
        return FONT.getWidth(clean.toString());
    }
}
