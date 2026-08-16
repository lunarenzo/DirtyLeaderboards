package me.romix.dirtyLeaderboards.skript;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.List;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.SourceType;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExprLeaderboardData extends SimpleExpression<Number> {

    public static final String[] PATTERNS = {
            "[the] leaderboard (data|score|value) (of|for) %offlineplayers% (in|for) [leaderboard] %string%",
            "[the] leaderboard (data|score|value) (of|for) [leaderboard] %string% (of|for) %offlineplayers%"
    };
    public static final ExpressionType TYPE = ExpressionType.COMBINED;

    private Expression<OfflinePlayer> players;
    private Expression<String> board;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed,
                        @NotNull ParseResult parseResult) {
        players = (Expression<OfflinePlayer>) exprs[matchedPattern == 0 ? 0 : 1];
        board = (Expression<String>) exprs[matchedPattern == 0 ? 1 : 0];
        return true;
    }

    @Override
    protected Number @NotNull [] get(@NotNull Event event) {
        String id = board.getSingle(event);
        DirtyLeaderboards plugin = DirtyLeaderboards.instance();
        if (id == null || plugin == null) {
            return new Number[0];
        }
        List<Number> values = new ArrayList<>();
        for (OfflinePlayer player : players.getArray(event)) {
            Double value = plugin.leaderboardValue(id, player);
            values.add(value == null ? 0 : value);
        }
        return values.toArray(new Number[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(@NotNull ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> new Class[]{Number.class};
            case DELETE, RESET -> new Class[0];
            default -> null;
        };
    }

    @Override
    public void change(@NotNull Event event, Object @Nullable [] delta, @NotNull ChangeMode mode) {
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
        double amount = delta != null && delta.length > 0 && delta[0] instanceof Number number
                ? number.doubleValue() : 0;
        for (OfflinePlayer player : players.getArray(event)) {
            switch (mode) {
                case SET -> plugin.scoreStore().set(type.id(), player.getUniqueId(), amount);
                case ADD -> plugin.scoreStore().add(type.id(), player.getUniqueId(), amount);
                case REMOVE -> plugin.scoreStore().add(type.id(), player.getUniqueId(), -amount);
                case DELETE, RESET -> plugin.scoreStore().remove(type.id(), player.getUniqueId());
                default -> {
                }
            }
        }
        plugin.topService().invalidate(type.id());
    }

    @Override
    public boolean isSingle() {
        return players.isSingle();
    }

    @Override
    public @NotNull Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "leaderboard data of " + players.toString(event, debug)
                + " in " + board.toString(event, debug);
    }
}
