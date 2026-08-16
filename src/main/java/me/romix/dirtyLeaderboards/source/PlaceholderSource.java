package me.romix.dirtyLeaderboards.source;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.clip.placeholderapi.PlaceholderAPI;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PlaceholderSource implements StatSource {

    private final Plugin plugin;
    private final int playersPerTick;

    public PlaceholderSource(Plugin plugin, int playersPerTick) {
        this.plugin = plugin;
        this.playersPerTick = Math.max(1, playersPerTick);
    }

    @Override
    public CompletableFuture<Map<UUID, Double>> collect(LeaderboardType type) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        CompletableFuture<Map<UUID, Double>> future = new CompletableFuture<>();
        Map<UUID, Double> values = new HashMap<>();
        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                int end = Math.min(players.length, index + playersPerTick);
                for (; index < end; index++) {
                    OfflinePlayer player = players[index];
                    Double value = parseNumeric(PlaceholderAPI.setPlaceholders(player, type.placeholder()));
                    if (value != null && value != 0) {
                        values.put(player.getUniqueId(), value);
                    }
                }
                if (index >= players.length) {
                    cancel();
                    future.complete(values);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return future;
    }

    public static Double parseNumeric(String resolved) {
        if (resolved == null || resolved.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(resolved.replace(",", "").replace(" ", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
