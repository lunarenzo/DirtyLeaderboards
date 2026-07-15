package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.display.LeaderboardDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffLeaderboardDisplay extends Effect {

    public static final String[] PATTERNS = {
            "start [the] leaderboard display %string%",
            "stop [the] leaderboard display %string%",
            "skip [the] leaderboard display %string%"
    };

    private Expression<String> display;
    private int action;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        display = (Expression<String>) exprs[0];
        action = matchedPattern;
        return true;
    }

    @Override
    protected void execute(@NotNull Event event) {
        String id = display.getSingle(event);
        DirtyLeaderboards plugin = DirtyLeaderboards.instance();
        if (id == null || plugin == null) {
            return;
        }
        LeaderboardDisplay target = plugin.displayManager().get(id);
        if (target == null) {
            plugin.getLogger().warning("Skript referenced unknown leaderboard display '" + id + "'");
            return;
        }
        switch (action) {
            case 0 -> target.start();
            case 1 -> target.stop();
            default -> target.skip();
        }
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        String verb = action == 0 ? "start" : action == 1 ? "stop" : "skip";
        return verb + " leaderboard display " + display.toString(event, debug);
    }
}
