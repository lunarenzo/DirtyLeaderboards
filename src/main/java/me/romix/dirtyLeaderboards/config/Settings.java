package me.romix.dirtyLeaderboards.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.ValueFormat;
import me.romix.dirtyLeaderboards.text.NumberFormatter;
import me.romix.dirtyLeaderboards.text.PixelWidth;
import me.romix.dirtyLeaderboards.text.TimeFormatter;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

    private static final Map<String, Long> TIME_UNIT_SECONDS = Map.of(
            "month", 2592000L,
            "week", 604800L,
            "day", 86400L,
            "hour", 3600L,
            "minute", 60L,
            "second", 1L
    );
    private static final List<String> TIME_UNIT_ORDER =
            List.of("month", "week", "day", "hour", "minute", "second");

    private final long cacheMillis;
    private final List<String> excludedNamePrefixes;
    private final int placeholderPlayersPerTick;
    private final boolean updateCheckerEnabled;
    private final long updateCheckIntervalTicks;
    private final NumberFormatter numberFormatter;
    private final TimeFormatter timeFormatter;
    private final Map<String, DisplayConfig> displays;
    private final List<String> warnings;

    private Settings(long cacheMillis, List<String> excludedNamePrefixes, int placeholderPlayersPerTick,
                     boolean updateCheckerEnabled, long updateCheckIntervalTicks,
                     NumberFormatter numberFormatter, TimeFormatter timeFormatter,
                     Map<String, DisplayConfig> displays, List<String> warnings) {
        this.cacheMillis = cacheMillis;
        this.excludedNamePrefixes = excludedNamePrefixes;
        this.placeholderPlayersPerTick = placeholderPlayersPerTick;
        this.updateCheckerEnabled = updateCheckerEnabled;
        this.updateCheckIntervalTicks = updateCheckIntervalTicks;
        this.numberFormatter = numberFormatter;
        this.timeFormatter = timeFormatter;
        this.displays = displays;
        this.warnings = warnings;
    }

    public static Settings load(FileConfiguration config) {
        List<String> warnings = new ArrayList<>();
        long cacheMillis = Math.max(1, config.getInt("settings.cache-seconds", 30)) * 1000L;
        List<String> prefixes = List.copyOf(config.getStringList("settings.excluded-name-prefixes"));
        int perTick = Math.max(1, config.getInt("settings.placeholder-players-per-tick", 200));
        boolean updateCheckerEnabled = config.getBoolean("update-checker.enabled", true);
        double intervalHours = Math.max(0.25, config.getDouble("update-checker.check-interval-hours", 12.0));
        long updateCheckIntervalTicks = (long) (intervalHours * 3600.0 * 20.0);
        NumberFormatter numberFormatter = new NumberFormatter(
                config.getString("formatting.numbers.grouping", ","),
                config.getLong("formatting.numbers.compact-from", 10000L),
                config.getStringList("formatting.numbers.compact-suffixes"));
        int maxTimeUnits = Math.max(1, config.getInt("formatting.time.max-units", 2));
        TimeFormatter timeFormatter = new TimeFormatter(
                loadTimeUnits(config),
                config.getString("formatting.time.separator", ", "),
                maxTimeUnits);
        Map<String, DisplayConfig> displays = new LinkedHashMap<>();
        ConfigurationSection displaysSection = config.getConfigurationSection("displays");
        if (displaysSection != null) {
            for (String id : displaysSection.getKeys(false)) {
                ConfigurationSection section = displaysSection.getConfigurationSection(id);
                if (section == null) {
                    warnings.add("display '" + id + "' is not a section, skipped");
                    continue;
                }
                displays.put(id, loadDisplay(id, section, warnings));
            }
        }
        return new Settings(cacheMillis, prefixes, perTick, updateCheckerEnabled, updateCheckIntervalTicks,
                numberFormatter, timeFormatter, displays, warnings);
    }

    private static List<TimeFormatter.Unit> loadTimeUnits(FileConfiguration config) {
        List<TimeFormatter.Unit> units = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("formatting.time.units");
        if (section == null) {
            return List.of(
                    new TimeFormatter.Unit(86400, "d"),
                    new TimeFormatter.Unit(3600, "h"),
                    new TimeFormatter.Unit(60, "m"),
                    new TimeFormatter.Unit(1, "s"));
        }
        for (String unit : TIME_UNIT_ORDER) {
            String suffix = section.getString(unit);
            if (suffix != null && !suffix.isBlank()) {
                units.add(new TimeFormatter.Unit(TIME_UNIT_SECONDS.get(unit), suffix));
            }
        }
        return units;
    }

    private static DisplayConfig loadDisplay(String id, ConfigurationSection section, List<String> warnings) {
        ConfigurationSection billboard = section.getConfigurationSection("billboard");
        ConfigurationSection progressBar = section.getConfigurationSection("progress-bar");
        ConfigurationSection style = section.getConfigurationSection("style");
        return new DisplayConfig(
                id,
                section.getBoolean("auto-start", true),
                section.getBoolean("force-load-chunks", true),
                section.getString("location.world", "world"),
                section.getDouble("location.x", 0),
                section.getDouble("location.y", 100),
                section.getDouble("location.z", 0),
                (float) section.getDouble("location.yaw", 0),
                List.copyOf(section.getStringList("rotation")),
                loadBillboard(id, billboard, warnings),
                loadProgressBar(id, progressBar, warnings),
                loadStyle(style)
        );
    }

    private static DisplayConfig.Billboard loadBillboard(String id, ConfigurationSection section,
                                                         List<String> warnings) {
        if (section == null) {
            return new DisplayConfig.Billboard(true,
                    blockData(id, "black_concrete", "black_concrete", warnings),
                    blockData(id, "white_concrete", "white_concrete", warnings),
                    5.25, 3.7, -0.2, 0.1);
        }
        return new DisplayConfig.Billboard(
                section.getBoolean("enabled", true),
                blockData(id, section.getString("background-block", "black_concrete"), "black_concrete", warnings),
                blockData(id, section.getString("border-block", "white_concrete"), "white_concrete", warnings),
                section.getDouble("width", 5.25),
                section.getDouble("height", 3.7),
                section.getDouble("bottom-offset", -0.2),
                section.getDouble("border-thickness", 0.1)
        );
    }

    private static DisplayConfig.ProgressBar loadProgressBar(String id, ConfigurationSection section,
                                                             List<String> warnings) {
        if (section == null) {
            return new DisplayConfig.ProgressBar(true,
                    blockData(id, "gray_concrete", "gray_concrete", warnings),
                    blockData(id, "white_concrete", "white_concrete", warnings),
                    4.0, 0.05);
        }
        return new DisplayConfig.ProgressBar(
                section.getBoolean("enabled", true),
                blockData(id, section.getString("background-block", "gray_concrete"), "gray_concrete", warnings),
                blockData(id, section.getString("foreground-block", "white_concrete"), "white_concrete", warnings),
                section.getDouble("length", 4.0),
                section.getDouble("width", 0.05)
        );
    }

    private static DisplayStyle loadStyle(ConfigurationSection section) {
        if (section == null) {
            section = new org.bukkit.configuration.MemoryConfiguration();
        }
        int maxTitlePixelWidth = section.getInt("max-title-pixel-width", 0);
        if (maxTitlePixelWidth <= 0) {
            maxTitlePixelWidth = PixelWidth.of(" TOP DEATHS");
        }
        List<String> rankColors = section.getStringList("rank-colors");
        if (rankColors.isEmpty()) {
            rankColors = List.of("#fffb00", "#cfcfcf", "#d3854d");
        }
        return new DisplayStyle(
                section.getString("name-color", "white"),
                section.getString("online-name-format", "<gradient:#A745FF:#cf97ff:#A745FF><name></gradient>"),
                section.getString("dots-color", "#080A0F"),
                List.copyOf(rankColors),
                section.getString("default-rank-color", "#aaaaaa"),
                section.getInt("max-pixel-width", 225),
                section.getInt("max-name-pixel-width", 100),
                maxTitlePixelWidth,
                section.getInt("head-sprite-width", 8)
        );
    }

    private static BlockData blockData(String displayId, String name, String fallback, List<String> warnings) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        if (material == null || !material.isBlock()) {
            warnings.add("display '" + displayId + "' uses unknown block '" + name
                    + "', falling back to " + fallback);
            material = Material.matchMaterial(fallback);
        }
        return material.createBlockData();
    }

    public long cacheMillis() {
        return cacheMillis;
    }

    public List<String> excludedNamePrefixes() {
        return excludedNamePrefixes;
    }

    public int placeholderPlayersPerTick() {
        return placeholderPlayersPerTick;
    }

    public boolean updateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public long updateCheckIntervalTicks() {
        return updateCheckIntervalTicks;
    }

    public String formatValue(LeaderboardType type, double value) {
        if (type.format() == ValueFormat.TIME) {
            return timeFormatter.formatTicks((long) value);
        }
        return numberFormatter.format(value);
    }

    public NumberFormatter numberFormatter() {
        return numberFormatter;
    }

    public TimeFormatter timeFormatter() {
        return timeFormatter;
    }

    public Map<String, DisplayConfig> displays() {
        return displays;
    }

    public List<String> warnings() {
        return warnings;
    }
}
