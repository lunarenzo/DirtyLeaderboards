package me.romix.dirtyLeaderboards.display;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import me.romix.dirtyLeaderboards.config.DisplayConfig;
import me.romix.dirtyLeaderboards.config.Settings;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardRegistry;
import me.romix.dirtyLeaderboards.leaderboard.TopService;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class DisplayManager {

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final Settings settings;
    private final LeaderboardRegistry registry;
    private final TopService topService;
    private final Map<String, LeaderboardDisplay> displays = new LinkedHashMap<>();

    public DisplayManager(Plugin plugin, NamespacedKey markerKey, Settings settings,
                          LeaderboardRegistry registry, TopService topService) {
        this.plugin = plugin;
        this.markerKey = markerKey;
        this.settings = settings;
        this.registry = registry;
        this.topService = topService;
        for (DisplayConfig config : settings.displays().values()) {
            displays.put(config.id(), create(config));
        }
    }

    private LeaderboardDisplay create(DisplayConfig config) {
        return new LeaderboardDisplay(plugin, markerKey, config, registry, topService,
                new RowRenderer(settings, config.style()));
    }

    public int startAutoStarting() {
        int started = 0;
        for (LeaderboardDisplay display : displays.values()) {
            if (display.config().autoStart() && display.start()) {
                started++;
            }
        }
        return started;
    }

    public void stopAll() {
        for (LeaderboardDisplay display : displays.values()) {
            display.stop();
        }
    }

    public LeaderboardDisplay get(String id) {
        return id == null ? null : displays.get(id);
    }

    public Set<String> ids() {
        return displays.keySet();
    }

    public Collection<LeaderboardDisplay> all() {
        return displays.values();
    }

    public boolean move(String id, Location location) {
        LeaderboardDisplay display = displays.get(id);
        if (display == null) {
            return false;
        }
        boolean shouldRun = display.isRunning() || display.config().autoStart();
        display.stop();
        DisplayConfig moved = display.config().withLocation(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw());
        LeaderboardDisplay replacement = create(moved);
        displays.put(id, replacement);
        if (shouldRun) {
            replacement.start();
        }
        return true;
    }
}
