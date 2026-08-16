package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.display.LeaderboardDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CondDisplayRunning extends Condition {

    public static final String[] PATTERNS = {
            "[the] leaderboard display %string% is running",
            "[the] leaderboard display %string% is(n't| not) running"
    };

    private Expression<String> display;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        display = (Expression<String>) exprs[0];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(@NotNull Event event) {
        String id = display.getSingle(event);
        DirtyLeaderboards plugin = DirtyLeaderboards.instance();
        LeaderboardDisplay target = id == null || plugin == null ? null : plugin.displayManager().get(id);
        boolean running = target != null && target.isRunning();
        return running != isNegated();
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "leaderboard display " + display.toString(event, debug)
                + (isNegated() ? " is not running" : " is running");
    }
}
