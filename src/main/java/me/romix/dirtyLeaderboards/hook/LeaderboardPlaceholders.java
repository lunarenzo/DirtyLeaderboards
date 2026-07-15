package me.romix.dirtyLeaderboards.hook;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.TopEntry;
import me.romix.dirtyLeaderboards.leaderboard.TopService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class LeaderboardPlaceholders extends PlaceholderExpansion {

    private final DirtyLeaderboards plugin;

    public LeaderboardPlaceholders(DirtyLeaderboards plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dirtyleaderboards";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);
        if (lower.startsWith("top_")) {
            return top(lower.substring(4));
        }
        if (lower.startsWith("value_")) {
            return value(player, lower.substring(6), true);
        }
        if (lower.startsWith("raw_")) {
            return value(player, lower.substring(4), false);
        }
        return null;
    }

    private String top(String params) {
        String[] parts = params.split("_");
        if (parts.length < 3) {
            return null;
        }
        String field = parts[parts.length - 1];
        int rank;
        try {
            rank = Integer.parseInt(parts[parts.length - 2]);
        } catch (NumberFormatException e) {
            return null;
        }
        String board = String.join("_", Arrays.copyOfRange(parts, 0, parts.length - 2));
        LeaderboardType type = plugin.registry().get(board);
        if (type == null || rank < 1 || rank > TopService.TOP_SIZE) {
            return null;
        }
        if (Bukkit.isPrimaryThread()) {
            plugin.topService().prefetch(type);
        }
        List<TopEntry> entries = plugin.topService().cachedTop(type.id());
        if (rank > entries.size()) {
            return field.equals("name") ? "Player" : "0";
        }
        TopEntry entry = entries.get(rank - 1);
        return switch (field) {
            case "name" -> entry.name() == null || entry.name().isBlank() ? "Player" : entry.name();
            case "value" -> plugin.settings().formatValue(type, entry.value());
            case "raw" -> raw(entry.value());
            default -> null;
        };
    }

    private String value(OfflinePlayer player, String board, boolean formatted) {
        LeaderboardType type = plugin.registry().get(board);
        if (type == null) {
            return null;
        }
        Double value = plugin.leaderboardValue(board, player);
        if (value == null) {
            return null;
        }
        return formatted ? plugin.settings().formatValue(type, value) : raw(value);
    }

    private String raw(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
