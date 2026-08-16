package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.SourceType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffClearLeaderboard extends Effect {

    public static final String[] PATTERNS = {
            "(clear|reset) [the] leaderboard [data] (of|for) leaderboard %string%"
    };

    private Expression<String> board;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        board = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected void execute(@NotNull Event event) {
        String id = board.getSingle(event);
        DirtyLeaderboards plugin = DirtyLeaderboards.instance();
        if (id == null || plugin == null) {
            return;
        }
        LeaderboardType type = plugin.registry().get(id);
        if (type == null || type.source() != SourceType.CUSTOM) {
            plugin.warnNonCustom(id);
            return;
        }
        plugin.scoreStore().clear(type.id());
        plugin.topService().invalidate(type.id());
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "clear leaderboard data of leaderboard " + board.toString(event, debug);
    }
}
