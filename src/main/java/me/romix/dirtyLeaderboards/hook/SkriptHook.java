package me.romix.dirtyLeaderboards.hook;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.api.LeaderboardRotateEvent;
import me.romix.dirtyLeaderboards.skript.CondDisplayRunning;
import me.romix.dirtyLeaderboards.skript.EffClearLeaderboard;
import me.romix.dirtyLeaderboards.skript.EffLeaderboardDisplay;
import me.romix.dirtyLeaderboards.skript.ExprCurrentLeaderboard;
import me.romix.dirtyLeaderboards.skript.ExprLeaderboardData;
import me.romix.dirtyLeaderboards.skript.ExprRotateDetail;

public final class SkriptHook {

    private SkriptHook() {
    }

    public static boolean register(DirtyLeaderboards plugin) {
        try {
            Skript.registerAddon(plugin);
            Skript.registerExpression(ExprLeaderboardData.class, Number.class,
                    ExprLeaderboardData.TYPE, ExprLeaderboardData.PATTERNS);
            Skript.registerExpression(ExprCurrentLeaderboard.class, String.class,
                    ExprCurrentLeaderboard.TYPE, ExprCurrentLeaderboard.PATTERNS);
            Skript.registerExpression(ExprRotateDetail.class, String.class,
                    ExprRotateDetail.TYPE, ExprRotateDetail.PATTERNS);
            Skript.registerEffect(EffLeaderboardDisplay.class, EffLeaderboardDisplay.PATTERNS);
            Skript.registerEffect(EffClearLeaderboard.class, EffClearLeaderboard.PATTERNS);
            Skript.registerCondition(CondDisplayRunning.class, CondDisplayRunning.PATTERNS);
            Skript.registerEvent("Leaderboard Rotate", SimpleEvent.class, LeaderboardRotateEvent.class,
                    "[dirty] leaderboard (rotate|rotation|cycle)");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Could not register the Skript integration", t);
            return false;
        }
    }
}
