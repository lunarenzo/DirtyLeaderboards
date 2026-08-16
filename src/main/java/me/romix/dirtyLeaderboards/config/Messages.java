package me.romix.dirtyLeaderboards.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import me.romix.dirtyLeaderboards.text.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class Messages {

    private final Logger logger;
    private final MiniMessage mini;
    private final ConfigurationSection messages;

    private Messages(Logger logger, MiniMessage mini, ConfigurationSection messages) {
        this.logger = logger;
        this.mini = mini;
        this.messages = messages;
    }

    public static Messages load(FileConfiguration config, Logger logger) {
        TagResolver.Builder theme = TagResolver.builder();
        ConfigurationSection themeSection = config.getConfigurationSection("theme");
        if (themeSection != null) {
            for (String key : themeSection.getKeys(false)) {
                TextColor color = Colors.parse(themeSection.getString(key, ""));
                if (color == null) {
                    logger.warning("messages.yml theme color '" + key + "' is invalid and was ignored");
                    continue;
                }
                theme.tag(key.toLowerCase(Locale.ROOT), Tag.styling(color));
            }
        }
        TagResolver themeResolver = theme.build();
        MiniMessage themed = MiniMessage.builder()
                .tags(TagResolver.resolver(TagResolver.standard(), themeResolver))
                .build();
        Component prefix = themed.deserialize(config.getString("prefix", ""));
        MiniMessage mini = MiniMessage.builder()
                .tags(TagResolver.resolver(
                        TagResolver.standard(),
                        themeResolver,
                        TagResolver.resolver("prefix", Tag.selfClosingInserting(prefix))))
                .build();
        return new Messages(logger, mini, config.getConfigurationSection("messages"));
    }

    public Component parse(String raw, TagResolver... placeholders) {
        try {
            return mini.deserialize(raw, placeholders);
        } catch (Exception e) {
            logger.warning("Invalid MiniMessage: " + raw);
            return Component.text(raw);
        }
    }

    public Component msg(String key, TagResolver... placeholders) {
        String raw = messages == null ? null : messages.getString(key);
        if (raw == null) {
            logger.warning("messages.yml is missing message '" + key + "'");
            return Component.text(key);
        }
        return parse(raw, placeholders);
    }

    public void send(CommandSender to, String key, TagResolver... placeholders) {
        to.sendMessage(msg(key, placeholders));
    }

    public List<Component> help(TagResolver... placeholders) {
        List<Component> lines = new ArrayList<>();
        if (messages != null) {
            for (String raw : messages.getStringList("help")) {
                lines.add(parse(raw, placeholders));
            }
        }
        return lines;
    }
}
