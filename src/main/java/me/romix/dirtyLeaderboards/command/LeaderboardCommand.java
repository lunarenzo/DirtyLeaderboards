package me.romix.dirtyLeaderboards.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import me.romix.dirtyLeaderboards.config.Messages;
import me.romix.dirtyLeaderboards.display.DisplayManager;
import me.romix.dirtyLeaderboards.display.LeaderboardDisplay;
import me.romix.dirtyLeaderboards.update.UpdateChecker;
import me.romix.dirtyLeaderboards.wand.WandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LeaderboardCommand implements TabExecutor {

    private static final List<String> SUB_COMMANDS =
            List.of("help", "list", "reload", "start", "stop", "skip", "move", "wand", "create", "setwall");
    private static final List<String> DISPLAY_SUB_COMMANDS =
            List.of("start", "stop", "skip", "move", "setwall");

    private final DirtyLeaderboards plugin;

    public LeaderboardCommand(DirtyLeaderboards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            case "start" -> withDisplay(sender, args, this::start);
            case "stop" -> withDisplay(sender, args, this::stop);
            case "skip" -> withDisplay(sender, args, this::skip);
            case "move" -> withDisplay(sender, args, this::move);
            case "wand" -> wand(sender);
            case "create" -> create(sender, args);
            case "setwall" -> setwall(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void wand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        player.getInventory().addItem(plugin.wandManager().createWandItem());
        plugin.messages().send(sender, "wand-given");
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "create-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        WandManager.WandSelection selection = plugin.wandManager().getSelection(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            plugin.messages().send(sender, "wand-no-selection");
            return;
        }
        WandManager.SelectionResult wall = plugin.wandManager().calculateWall(selection);
        if (wall == null) {
            plugin.messages().send(sender, "wand-invalid-selection");
            return;
        }
        List<String> rotation = args.length >= 3 ? List.of(args[2].split(",")) : List.of();
        plugin.configManager().saveDisplayWall(id, wall.location(), wall.width(), wall.height(), rotation);
        plugin.reloadEverything();
        plugin.wandManager().clearSelection(player.getUniqueId());
        plugin.messages().send(sender, "display-created", Placeholder.unparsed("display", id));
    }

    private void setwall(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "setwall-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        WandManager.WandSelection selection = plugin.wandManager().getSelection(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            plugin.messages().send(sender, "wand-no-selection");
            return;
        }
        WandManager.SelectionResult wall = plugin.wandManager().calculateWall(selection);
        if (wall == null) {
            plugin.messages().send(sender, "wand-invalid-selection");
            return;
        }
        plugin.configManager().saveDisplayWall(id, wall.location(), wall.width(), wall.height(), List.of());
        plugin.reloadEverything();
        plugin.wandManager().clearSelection(player.getUniqueId());
        plugin.messages().send(sender, "display-setwall", Placeholder.unparsed("display", id));
    }

    private void help(CommandSender sender) {
        Messages messages = plugin.messages();
        String current = plugin.getPluginMeta().getVersion();
        for (Component line : messages.help(Placeholder.unparsed("version", current))) {
            sender.sendMessage(line);
        }
        UpdateChecker checker = plugin.updateChecker();
        String latest = checker == null ? null : checker.availableVersion();
        if (latest == null) {
            return;
        }
        messages.send(sender, "update-available",
                Placeholder.unparsed("current", current),
                Placeholder.unparsed("latest", latest));
        Component link = messages.msg("update-link", Placeholder.unparsed("url", checker.downloadUrl()))
                .hoverEvent(HoverEvent.showText(messages.msg("update-link-hover")))
                .clickEvent(ClickEvent.openUrl(checker.downloadUrl()));
        sender.sendMessage(link);
    }

    private void reload(CommandSender sender) {
        long startedAt = System.currentTimeMillis();
        boolean success = plugin.reloadEverything();
        if (success) {
            plugin.messages().send(sender, "reloaded", Placeholder.unparsed("time",
                    String.valueOf(System.currentTimeMillis() - startedAt)));
        } else {
            plugin.messages().send(sender, "reload-failed");
        }
    }

    private void list(CommandSender sender) {
        Messages messages = plugin.messages();
        messages.send(sender, "list-header");
        for (LeaderboardDisplay display : plugin.displayManager().all()) {
            if (display.isRunning()) {
                messages.send(sender, "list-entry",
                        Placeholder.unparsed("display", display.config().id()),
                        Placeholder.component("status", messages.msg("status-running")),
                        Placeholder.unparsed("leaderboard", String.valueOf(display.currentLeaderboardId())));
            } else {
                messages.send(sender, "list-entry-stopped",
                        Placeholder.unparsed("display", display.config().id()),
                        Placeholder.component("status", messages.msg("status-stopped")));
            }
        }
    }

    private void withDisplay(CommandSender sender, String[] args, DisplayAction action) {
        DisplayManager manager = plugin.displayManager();
        if (manager.ids().isEmpty()) {
            plugin.messages().send(sender, "no-displays");
            return;
        }
        String id;
        if (args.length >= 2) {
            id = args[1];
        } else if (manager.ids().size() == 1) {
            id = manager.ids().iterator().next();
        } else {
            plugin.messages().send(sender, "specify-display",
                    Placeholder.unparsed("displays", String.join(", ", manager.ids())));
            return;
        }
        LeaderboardDisplay display = manager.get(id);
        if (display == null) {
            plugin.messages().send(sender, "unknown-display", Placeholder.unparsed("display", id));
            return;
        }
        action.run(sender, display);
    }

    private void start(CommandSender sender, LeaderboardDisplay display) {
        String id = display.config().id();
        if (display.isRunning()) {
            plugin.messages().send(sender, "display-already-running", Placeholder.unparsed("display", id));
            return;
        }
        String key = display.start() ? "display-started" : "display-start-failed";
        plugin.messages().send(sender, key, Placeholder.unparsed("display", id));
    }

    private void stop(CommandSender sender, LeaderboardDisplay display) {
        String id = display.config().id();
        if (!display.isRunning()) {
            plugin.messages().send(sender, "display-not-running", Placeholder.unparsed("display", id));
            return;
        }
        display.stop();
        plugin.messages().send(sender, "display-stopped", Placeholder.unparsed("display", id));
    }

    private void skip(CommandSender sender, LeaderboardDisplay display) {
        String id = display.config().id();
        String next = display.skip();
        if (next == null) {
            plugin.messages().send(sender, "display-not-running", Placeholder.unparsed("display", id));
            return;
        }
        plugin.messages().send(sender, "display-skipped",
                Placeholder.unparsed("display", id),
                Placeholder.unparsed("leaderboard", next));
    }

    private void move(CommandSender sender, LeaderboardDisplay display) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        String id = display.config().id();
        plugin.configManager().saveDisplayLocation(id, player.getLocation());
        plugin.displayManager().move(id, player.getLocation());
        plugin.messages().send(sender, "display-moved", Placeholder.unparsed("display", id));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            return filter(SUB_COMMANDS, args[0]);
        }
        if (args.length == 2 && DISPLAY_SUB_COMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(new ArrayList<>(plugin.displayManager().ids()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }

    @FunctionalInterface
    private interface DisplayAction {
        void run(CommandSender sender, LeaderboardDisplay display);
    }
}
