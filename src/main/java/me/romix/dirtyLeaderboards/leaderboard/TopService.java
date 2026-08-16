package me.romix.dirtyLeaderboards.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.config.Settings;
import me.romix.dirtyLeaderboards.source.CustomSource;
import me.romix.dirtyLeaderboards.source.PlaceholderSource;
import me.romix.dirtyLeaderboards.source.StatSource;
import me.romix.dirtyLeaderboards.source.StatisticSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class TopService {

    public static final int TOP_SIZE = 10;

    private final Plugin plugin;
    private final Executor io;
    private final Settings settings;
    private final Map<SourceType, StatSource> sources = new EnumMap<>(SourceType.class);
    private final Map<String, CachedTop> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<TopEntry>>> pending = new HashMap<>();

    private record CachedTop(List<TopEntry> entries, long timestamp) {
    }

    public TopService(Plugin plugin, Executor io, Settings settings, ScoreStore store, boolean placeholderApi) {
        this.plugin = plugin;
        this.io = io;
        this.settings = settings;
        sources.put(SourceType.STATISTIC, new StatisticSource(plugin, io));
        sources.put(SourceType.CUSTOM, new CustomSource(store));
        if (placeholderApi) {
            sources.put(SourceType.PLACEHOLDER, new PlaceholderSource(plugin, settings.placeholderPlayersPerTick()));
        }
    }

    public CompletableFuture<List<TopEntry>> top(LeaderboardType type) {
        CachedTop cached = cache.get(type.id());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < settings.cacheMillis()) {
            return CompletableFuture.completedFuture(cached.entries());
        }
        CompletableFuture<List<TopEntry>> running = pending.get(type.id());
        if (running != null) {
            return running;
        }
        return refresh(type);
    }

    public void prefetch(LeaderboardType type) {
        top(type);
    }

    public List<TopEntry> cachedTop(String id) {
        CachedTop cached = cache.get(id);
        return cached == null ? List.of() : cached.entries();
    }

    public void invalidate(String id) {
        cache.remove(id);
    }

    private CompletableFuture<List<TopEntry>> refresh(LeaderboardType type) {
        StatSource source = sources.get(type.source());
        if (source == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        CompletableFuture<List<TopEntry>> result = new CompletableFuture<>();
        pending.put(type.id(), result);
        source.collect(type)
                .thenApplyAsync(values -> select(type, values), io)
                .whenComplete((entries, error) -> onMainThread(() -> {
                    pending.remove(type.id());
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING,
                                "Could not collect leaderboard '" + type.id() + "'", error);
                        CachedTop stale = cache.get(type.id());
                        result.complete(stale == null ? List.of() : stale.entries());
                        return;
                    }
                    cache.put(type.id(), new CachedTop(entries, System.currentTimeMillis()));
                    result.complete(entries);
                }));
        return result;
    }

    private List<TopEntry> select(LeaderboardType type, Map<UUID, Double> values) {
        boolean ascending = type.sortOrder() == SortOrder.ASCENDING;
        Comparator<TopEntry> worstFirst = Comparator.comparingDouble(TopEntry::value);
        if (ascending) {
            worstFirst = worstFirst.reversed();
        }
        PriorityQueue<TopEntry> best = new PriorityQueue<>(worstFirst);
        for (Map.Entry<UUID, Double> entry : values.entrySet()) {
            double value = entry.getValue();
            if (value < type.minimumValue()) {
                continue;
            }
            if (best.size() >= TOP_SIZE) {
                double worstKept = best.peek().value();
                if (ascending ? value >= worstKept : value <= worstKept) {
                    continue;
                }
            }
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (isExcluded(name)) {
                continue;
            }
            best.add(new TopEntry(entry.getKey(), name, value));
            if (best.size() > TOP_SIZE) {
                best.poll();
            }
        }
        List<TopEntry> entries = new ArrayList<>(best);
        entries.sort(worstFirst.reversed());
        return List.copyOf(entries);
    }

    private boolean isExcluded(String name) {
        if (name == null) {
            return false;
        }
        for (String prefix : settings.excludedNamePrefixes()) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void onMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, runnable);
        } catch (IllegalStateException e) {
            runnable.run();
        }
    }
}
