package me.romix.dirtyLeaderboards.leaderboard;

import java.util.UUID;

public record TopEntry(UUID uuid, String name, double value) {
}
