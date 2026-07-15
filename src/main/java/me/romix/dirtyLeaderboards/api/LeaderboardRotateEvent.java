package me.romix.dirtyLeaderboards.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LeaderboardRotateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String displayId;
    private final String leaderboardId;

    public LeaderboardRotateEvent(String displayId, String leaderboardId) {
        this.displayId = displayId;
        this.leaderboardId = leaderboardId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getLeaderboardId() {
        return leaderboardId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
