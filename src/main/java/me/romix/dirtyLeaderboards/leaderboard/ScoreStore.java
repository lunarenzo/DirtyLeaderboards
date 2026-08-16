package me.romix.dirtyLeaderboards.leaderboard;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.configuration.ConfigurationSection;

public final class ScoreStore {

    private final Map<String, Map<UUID, Double>> data = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public static ScoreStore load(ConfigurationSection root) {
        ScoreStore store = new ScoreStore();
        ConfigurationSection section = root.getConfigurationSection("data");
        if (section == null) {
            return store;
        }
        for (String board : section.getKeys(false)) {
            ConfigurationSection scores = section.getConfigurationSection(board);
            if (scores == null) {
                continue;
            }
            Map<UUID, Double> values = store.board(board.toLowerCase(Locale.ROOT));
            for (String key : scores.getKeys(false)) {
                try {
                    values.put(UUID.fromString(key), scores.getDouble(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return store;
    }

    private Map<UUID, Double> board(String board) {
        return data.computeIfAbsent(board, k -> new ConcurrentHashMap<>());
    }

    public double get(String board, UUID uuid) {
        Map<UUID, Double> values = data.get(board.toLowerCase(Locale.ROOT));
        if (values == null) {
            return 0;
        }
        return values.getOrDefault(uuid, 0.0);
    }

    public void set(String board, UUID uuid, double value) {
        board(board.toLowerCase(Locale.ROOT)).put(uuid, value);
        dirty.set(true);
    }

    public void add(String board, UUID uuid, double delta) {
        board(board.toLowerCase(Locale.ROOT)).merge(uuid, delta, Double::sum);
        dirty.set(true);
    }

    public void remove(String board, UUID uuid) {
        Map<UUID, Double> values = data.get(board.toLowerCase(Locale.ROOT));
        if (values != null && values.remove(uuid) != null) {
            dirty.set(true);
        }
    }

    public void clear(String board) {
        Map<UUID, Double> values = data.get(board.toLowerCase(Locale.ROOT));
        if (values != null && !values.isEmpty()) {
            values.clear();
            dirty.set(true);
        }
    }

    public Map<UUID, Double> snapshot(String board) {
        Map<UUID, Double> values = data.get(board.toLowerCase(Locale.ROOT));
        return values == null ? Map.of() : Map.copyOf(values);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void writeTo(ConfigurationSection root) {
        root.set("data", null);
        ConfigurationSection section = root.createSection("data");
        for (Map.Entry<String, Map<UUID, Double>> board : data.entrySet()) {
            if (board.getValue().isEmpty()) {
                continue;
            }
            ConfigurationSection scores = section.createSection(board.getKey());
            for (Map.Entry<UUID, Double> entry : new HashMap<>(board.getValue()).entrySet()) {
                double value = entry.getValue();
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    scores.set(entry.getKey().toString(), (long) value);
                } else {
                    scores.set(entry.getKey().toString(), value);
                }
            }
        }
        dirty.set(false);
    }
}
