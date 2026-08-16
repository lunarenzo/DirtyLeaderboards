package me.romix.dirtyLeaderboards.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.romix.dirtyLeaderboards.source.PlaceholderSource;
import org.bukkit.OfflinePlayer;

public final class PlaceholderValueReader {

    private PlaceholderValueReader() {
    }

    public static Double read(OfflinePlayer player, String placeholder) {
        return PlaceholderSource.parseNumeric(PlaceholderAPI.setPlaceholders(player, placeholder));
    }
}
