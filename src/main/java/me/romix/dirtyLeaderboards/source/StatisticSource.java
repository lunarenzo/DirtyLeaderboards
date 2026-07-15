package me.romix.dirtyLeaderboards.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StatisticSource implements StatSource {

    private final Plugin plugin;
    private final Executor io;

    public StatisticSource(Plugin plugin, Executor io) {
        this.plugin = plugin;
        this.io = io;
    }

    @Override
    public CompletableFuture<Map<UUID, Double>> collect(LeaderboardType type) {
        Map<UUID, Double> values = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            values.put(player.getUniqueId(), (double) player.getStatistic(type.statistic()));
        }
        Path statsDirectory = Bukkit.getWorlds().getFirst().getWorldFolder().toPath().resolve("stats");
        String jsonKey = type.statisticJsonKey();
        return CompletableFuture.supplyAsync(() -> {
            readOfflineStats(statsDirectory, jsonKey, values);
            return values;
        }, io);
    }

    private void readOfflineStats(Path directory, String jsonKey, Map<UUID, Double> values) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                UUID uuid;
                try {
                    uuid = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (values.containsKey(uuid)) {
                    continue;
                }
                double value = readStat(file, jsonKey);
                if (value != 0) {
                    values.put(uuid, value);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not scan the statistics folder", e);
        }
    }

    private double readStat(Path file, String jsonKey) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return 0;
            }
            JsonElement stats = root.getAsJsonObject().get("stats");
            if (stats == null || !stats.isJsonObject()) {
                return 0;
            }
            JsonElement custom = stats.getAsJsonObject().get("minecraft:custom");
            if (custom == null || !custom.isJsonObject()) {
                return 0;
            }
            JsonObject customObject = custom.getAsJsonObject();
            JsonElement value = customObject.get(jsonKey);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                return 0;
            }
            return value.getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }
}
