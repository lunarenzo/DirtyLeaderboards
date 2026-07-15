package me.romix.dirtyLeaderboards;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.command.LeaderboardCommand;
import me.romix.dirtyLeaderboards.config.ConfigManager;
import me.romix.dirtyLeaderboards.config.Messages;
import me.romix.dirtyLeaderboards.config.Settings;
import me.romix.dirtyLeaderboards.display.DisplayManager;
import me.romix.dirtyLeaderboards.hook.LeaderboardPlaceholders;
import me.romix.dirtyLeaderboards.hook.PlaceholderValueReader;
import me.romix.dirtyLeaderboards.hook.SkriptHook;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardRegistry;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.ScoreStore;
import me.romix.dirtyLeaderboards.leaderboard.TopService;
import me.romix.dirtyLeaderboards.util.StartupBanner;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DirtyLeaderboards extends JavaPlugin {

    private static DirtyLeaderboards instance;

    private final Set<String> warnedBoards = new HashSet<>();

    private ConfigManager configManager;
    private Settings settings;
    private Messages messages;
    private LeaderboardRegistry registry;
    private ScoreStore scoreStore;
    private TopService topService;
    private DisplayManager displayManager;
    private ExecutorService ioExecutor;
    private NamespacedKey markerKey;
    private boolean placeholderApiPresent;

    public static DirtyLeaderboards instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        markerKey = new NamespacedKey(this, "display");
        ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DirtyLeaderboards-IO");
            thread.setDaemon(true);
            return thread;
        });
        placeholderApiPresent = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        int started;
        try {
            loadServices();
            started = displayManager.startAutoStarting();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not load the configuration, disabling", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registerCommand();
        boolean skriptHooked = getServer().getPluginManager().getPlugin("Skript") != null
                && SkriptHook.register(this);
        if (placeholderApiPresent) {
            new LeaderboardPlaceholders(this).register();
        }
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::autosave, 600L, 600L);
        printBanner(started, skriptHooked);
    }

    @Override
    public void onDisable() {
        if (displayManager != null) {
            displayManager.stopAll();
        }
        if (configManager != null && scoreStore != null && scoreStore.isDirty()) {
            configManager.saveScores(scoreStore);
        }
        if (ioExecutor != null) {
            ioExecutor.shutdown();
        }
        instance = null;
    }

    private void loadServices() {
        if (configManager == null) {
            configManager = new ConfigManager(this);
        }
        configManager.loadAll();
        settings = Settings.load(configManager.config());
        messages = Messages.load(configManager.messagesConfig(), getLogger());
        registry = LeaderboardRegistry.load(configManager.leaderboardsConfig(), placeholderApiPresent);
        scoreStore = ScoreStore.load(configManager.leaderboardsConfig());
        topService = new TopService(this, ioExecutor, settings, scoreStore, placeholderApiPresent);
        displayManager = new DisplayManager(this, markerKey, settings, registry, topService);
        warnedBoards.clear();
        for (String warning : settings.warnings()) {
            getLogger().warning(warning);
        }
        for (String warning : registry.warnings()) {
            getLogger().warning(warning);
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("dirtyleaderboards");
        if (command == null) {
            getLogger().severe("Command 'dirtyleaderboards' is missing from plugin.yml");
            return;
        }
        LeaderboardCommand executor = new LeaderboardCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public boolean reloadEverything() {
        try {
            displayManager.stopAll();
            if (scoreStore != null && scoreStore.isDirty()) {
                configManager.saveScores(scoreStore);
            }
            loadServices();
            displayManager.startAutoStarting();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Reload failed", e);
            return false;
        }
    }

    private void autosave() {
        if (scoreStore != null && scoreStore.isDirty()) {
            configManager.saveScores(scoreStore);
        }
    }

    private void printBanner(int startedDisplays, boolean skriptHooked) {
        StartupBanner banner = new StartupBanner();
        int warningCount = settings.warnings().size() + registry.warnings().size();
        if (warningCount == 0) {
            banner.ok("Configuration loaded");
        } else {
            banner.warn("Configuration loaded with <white>" + warningCount
                    + "</white> warning(s), see above");
        }
        banner.ok("Leaderboards <dark_gray>(<gray>" + String.join(", ", registry.ids())
                + "</gray>)</dark_gray>");
        banner.ok("Displays <dark_gray>(<gray>" + startedDisplays + " of "
                + settings.displays().size() + " started</gray>)</dark_gray>");
        if (skriptHooked) {
            banner.ok("Skript syntax registered");
        } else {
            banner.warn("Skript not found <dark_gray>(optional)</dark_gray>");
        }
        if (placeholderApiPresent) {
            banner.ok("PlaceholderAPI expansion registered");
        } else {
            banner.warn("PlaceholderAPI not found <dark_gray>(optional)</dark_gray>");
        }
        banner.print(this);
    }

    public Double leaderboardValue(String boardId, OfflinePlayer player) {
        LeaderboardType type = registry.get(boardId);
        if (type == null) {
            return null;
        }
        return switch (type.source()) {
            case CUSTOM -> scoreStore.get(type.id(), player.getUniqueId());
            case STATISTIC -> (double) player.getStatistic(type.statistic());
            case PLACEHOLDER -> placeholderApiPresent
                    ? PlaceholderValueReader.read(player, type.placeholder()) : null;
        };
    }

    public void warnNonCustom(String boardId) {
        if (warnedBoards.add(boardId)) {
            getLogger().warning("A script tried to change leaderboard '" + boardId
                    + "', but only leaderboards with 'source: custom' can be changed");
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public LeaderboardRegistry registry() {
        return registry;
    }

    public ScoreStore scoreStore() {
        return scoreStore;
    }

    public TopService topService() {
        return topService;
    }

    public DisplayManager displayManager() {
        return displayManager;
    }

    public boolean placeholderApiPresent() {
        return placeholderApiPresent;
    }
}
