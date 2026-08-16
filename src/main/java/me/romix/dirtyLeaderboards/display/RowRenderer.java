package me.romix.dirtyLeaderboards.display;

import me.romix.dirtyLeaderboards.config.DisplayStyle;
import me.romix.dirtyLeaderboards.config.Settings;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.TopEntry;
import me.romix.dirtyLeaderboards.text.Colors;
import me.romix.dirtyLeaderboards.text.PixelWidth;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class RowRenderer {

    private static final String RANK_EMOJIS = "①②③④⑤⑥⑦⑧⑨⑩";
    private static final String STEVE_HEAD = "entity/player/wide/steve";
    private static final String FILLER_COLOR = "#777777";
    private static final double BASE_TITLE_SCALE = 2.5;

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Settings settings;
    private final DisplayStyle style;

    public RowRenderer(Settings settings, DisplayStyle style) {
        this.settings = settings;
        this.style = style;
    }

    public Component realRow(LeaderboardType type, TopEntry entry, int rank) {
        String glyph = rankEmoji(rank);
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.uuid());
        boolean online = player.isOnline();
        String name = entry.name();
        if (name == null || name.isBlank()) {
            name = "Player";
            online = false;
        }
        name = truncate(sanitize(name));
        Component head = online || player.hasPlayedBefore()
                ? Component.object(ObjectContents.playerHead(entry.uuid()))
                : parse("<head:" + STEVE_HEAD + ":true>");
        String nameTag = online
                ? style.onlineNameFormat().replace("<name>", name)
                : "<" + style.nameColor() + ">" + name;
        String value = settings.formatValue(type, entry.value());

        int leftLength = PixelWidth.of(glyph + " ") + 3 + 1 + style.headSpriteWidth() + 1
                + PixelWidth.of(" " + name) + 1;
        int rightLength = 1 + PixelWidth.of(value) + type.iconWidth();

        Component left = parse("<color:" + style.rankColor(rank) + ">" + glyph
                        + "</color> <player_head> " + nameTag,
                Placeholder.component("player_head", head));
        Component middle = parse("<color:" + style.dotsColor() + "><shadow:" + style.dotsColor() + ":0>"
                + dots(leftLength, rightLength));
        Component right = parse("<color:" + type.valueColor() + ">" + value + " <white>" + type.icon(entry.value()));
        return Component.textOfChildren(left, middle, right);
    }

    public Component fillerRow(LeaderboardType type, int rank) {
        String glyph = rankEmoji(rank);
        int leftLength = PixelWidth.of(glyph) + 3 + style.headSpriteWidth() + PixelWidth.of("Player") - 1;
        int rightLength = 1 + PixelWidth.of("0") + type.iconWidth();

        Component left = parse("<color:" + style.defaultRankColor() + ">" + glyph
                + "</color> <head:" + STEVE_HEAD + ":true> <" + FILLER_COLOR + ">Player");
        Component middle = parse("<color:" + style.dotsColor() + ">" + dots(leftLength, rightLength));
        Component right = parse("<" + FILLER_COLOR + ">0");
        return Component.textOfChildren(left, middle, right);
    }

    public Component titleBase(LeaderboardType type) {
        return parse("<white>" + type.titleIcon());
    }

    public Component titleChar(LeaderboardType type, char c) {
        TextColor color = Colors.parse(type.titleColor());
        return Component.text(String.valueOf(c), color == null ? NamedTextColor.WHITE : color)
                .decorate(TextDecoration.BOLD);
    }

    public double titleScale(LeaderboardType type) {
        double fullWidth = type.iconWidth() + PixelWidth.of(" " + type.title());
        if (fullWidth > style.maxTitlePixelWidth()) {
            return BASE_TITLE_SCALE * style.maxTitlePixelWidth() / fullWidth;
        }
        return BASE_TITLE_SCALE;
    }

    private String dots(int leftLength, int rightLength) {
        int available = style.maxPixelWidth() - (leftLength + rightLength + 2);
        int count = Math.max(0, available / 2 - 1);
        return ".".repeat(count);
    }

    private String rankEmoji(int rank) {
        return String.valueOf(RANK_EMOJIS.charAt(Math.min(Math.max(rank, 1), RANK_EMOJIS.length()) - 1));
    }

    private String truncate(String name) {
        if (PixelWidth.of(name) <= style.maxNamePixelWidth()) {
            return name;
        }
        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (PixelWidth.of(truncated.toString() + c + "…") > style.maxNamePixelWidth()) {
                return truncated + "…";
                // … instead of ... because of their size, most texture packs don't change them
            }
            truncated.append(c);
        }
        return name;
    }

    private String sanitize(String name) {
        return name.replace("<", "").replace(">", "");
    }

    private Component parse(String raw, TagResolver... resolvers) {
        try {
            return mini.deserialize(raw, resolvers);
        } catch (Exception e) {
            return Component.text(mini.stripTags(raw, resolvers));
        }
    }
}
