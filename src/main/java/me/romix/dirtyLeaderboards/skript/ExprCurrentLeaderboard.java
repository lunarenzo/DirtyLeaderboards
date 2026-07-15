package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.display.LeaderboardDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExprCurrentLeaderboard extends SimpleExpression<String> {

    public static final String[] PATTERNS = {
            "[the] current leaderboard (of|for) [display] %string%"
    };
    public static final ExpressionType TYPE = ExpressionType.COMBINED;

    private Expression<String> display;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        display = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected String @NotNull [] get(@NotNull Event event) {
        String id = display.getSingle(event);
        DirtyLeaderboards plugin = DirtyLeaderboards.instance();
        LeaderboardDisplay target = id == null || plugin == null ? null : plugin.displayManager().get(id);
        String current = target == null ? null : target.currentLeaderboardId();
        return current == null ? new String[0] : new String[]{current};
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
        return "current leaderboard of display " + display.toString(event, debug);
    }
}
