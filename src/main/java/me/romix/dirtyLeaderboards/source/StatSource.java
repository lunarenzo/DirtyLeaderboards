package me.romix.dirtyLeaderboards.source;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;

public interface StatSource {

    CompletableFuture<Map<UUID, Double>> collect(LeaderboardType type);
}
