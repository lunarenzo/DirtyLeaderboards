package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.romix.dirtyLeaderboards.api.LeaderboardRotateEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExprRotateDetail extends SimpleExpression<String> {

    public static final String[] PATTERNS = {
            "[the] rotated leaderboard [id]",
            "[the] rotated display [id]"
    };
    public static final ExpressionType TYPE = ExpressionType.SIMPLE;

    private boolean wantsDisplay;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        if (!getParser().isCurrentEvent(LeaderboardRotateEvent.class)) {
            Skript.error("'rotated leaderboard/display' can only be used in a leaderboard rotate event");
            return false;
        }
        wantsDisplay = matchedPattern == 1;
        return true;
    }

    @Override
    protected String @NotNull [] get(@NotNull Event event) {
        if (!(event instanceof LeaderboardRotateEvent rotate)) {
            return new String[0];
        }
        return new String[]{wantsDisplay ? rotate.getDisplayId() : rotate.getLeaderboardId()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public @NotNull Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return wantsDisplay ? "rotated display" : "rotated leaderboard";
    }
}
