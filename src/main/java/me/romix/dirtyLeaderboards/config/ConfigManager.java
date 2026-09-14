package me.romix.dirtyLeaderboards.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.leaderboard.ScoreStore;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private final File configFile;
    private final File messagesFile;
    private final File leaderboardsFile;
    private final Object leaderboardsLock = new Object();

    private YamlConfiguration config;
    private YamlConfiguration messagesConfig;
    private YamlConfiguration leaderboardsConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.leaderboardsFile = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    public void loadAll() {
        saveDefault("config.yml", configFile);
        saveDefault("messages.yml", messagesFile);
        saveDefault("leaderboards.yml", leaderboardsFile);
        config = YamlConfiguration.loadConfiguration(configFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        synchronized (leaderboardsLock) {
            leaderboardsConfig = YamlConfiguration.loadConfiguration(leaderboardsFile);
        }
    }

    private void saveDefault(String name, File file) {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    public YamlConfiguration config() {
        return config;
    }

    public YamlConfiguration messagesConfig() {
        return messagesConfig;
    }

    public YamlConfiguration leaderboardsConfig() {
        return leaderboardsConfig;
    }

    public void saveDisplayLocation(String id, Location location) {
        String path = "displays." + id + ".location.";
        config.set(path + "world", location.getWorld().getName());
        config.set(path + "x", location.getX());
        config.set(path + "y", location.getY());
        config.set(path + "z", location.getZ());
        config.set(path + "yaw", (double) location.getYaw());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    public void saveDisplayWall(String id, Location location, double width, double height, java.util.List<String> rotation) {
        String base = "displays." + id;
        config.set(base + ".auto-start", true);
        config.set(base + ".force-load-chunks", true);
        config.set(base + ".location.world", location.getWorld().getName());
        config.set(base + ".location.x", location.getX());
        config.set(base + ".location.y", location.getY());
        config.set(base + ".location.z", location.getZ());
        config.set(base + ".location.yaw", (double) location.getYaw());
        if (rotation != null && !rotation.isEmpty()) {
            config.set(base + ".rotation", rotation);
        } else if (!config.contains(base + ".rotation")) {
            config.set(base + ".rotation", java.util.List.of("kills", "deaths", "playtime"));
        }
        if (!config.contains(base + ".billboard")) {
            config.set(base + ".billboard.enabled", true);
            config.set(base + ".billboard.background-block", "black_concrete");
            config.set(base + ".billboard.border-block", "white_concrete");
            config.set(base + ".billboard.bottom-offset", -0.2);
            config.set(base + ".billboard.border-thickness", 0.1);
        }
        config.set(base + ".billboard.width", width);
        config.set(base + ".billboard.height", height);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    public void saveScores(ScoreStore store) {
        synchronized (leaderboardsLock) {
            try {
                YamlConfiguration fresh = YamlConfiguration.loadConfiguration(leaderboardsFile);
                store.writeTo(fresh);
                fresh.save(leaderboardsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save leaderboards.yml", e);
            }
        }
    }
}
