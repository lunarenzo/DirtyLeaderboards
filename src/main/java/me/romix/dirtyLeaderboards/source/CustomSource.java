package me.romix.dirtyLeaderboards.source;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.ScoreStore;

public final class CustomSource implements StatSource {

    private final ScoreStore store;

    public CustomSource(ScoreStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Map<UUID, Double>> collect(LeaderboardType type) {
        return CompletableFuture.completedFuture(store.snapshot(type.id()));
    }
}
