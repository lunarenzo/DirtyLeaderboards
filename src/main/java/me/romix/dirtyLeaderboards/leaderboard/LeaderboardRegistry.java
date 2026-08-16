package me.romix.dirtyLeaderboards.leaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;

public final class LeaderboardRegistry {

    private static final Map<String, Statistic> VANILLA_ALIASES = buildVanillaAliases();

    private final Map<String, LeaderboardType> types;
    private final List<String> warnings;

    private LeaderboardRegistry(Map<String, LeaderboardType> types, List<String> warnings) {
        this.types = types;
        this.warnings = warnings;
    }

    public static LeaderboardRegistry load(ConfigurationSection root, boolean placeholderApiPresent) {
        Map<String, LeaderboardType> types = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        ConfigurationSection section = root.getConfigurationSection("leaderboards");
        if (section == null) {
            warnings.add("leaderboards.yml has no 'leaderboards' section, nothing was loaded");
            return new LeaderboardRegistry(types, warnings);
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                warnings.add("leaderboard '" + id + "' is not a section, skipped");
                continue;
            }
            if (!entry.getBoolean("enabled", true)) {
                continue;
            }
            LeaderboardType type = parse(id.toLowerCase(Locale.ROOT), entry, placeholderApiPresent, warnings);
            if (type != null) {
                types.put(type.id(), type);
            }
        }
        return new LeaderboardRegistry(types, warnings);
    }

    private static LeaderboardType parse(String id, ConfigurationSection entry,
                                         boolean placeholderApiPresent, List<String> warnings) {
        SourceType source = SourceType.parse(entry.getString("source", ""));
        if (source == null) {
            warnings.add("leaderboard '" + id + "' has an invalid source '"
                    + entry.getString("source") + "' (statistic, placeholder or custom), skipped");
            return null;
        }
        Statistic statistic = null;
        String jsonKey = null;
        String placeholder = null;
        if (source == SourceType.STATISTIC) {
            String rawStatistic = entry.getString("statistic", "");
            statistic = resolveStatistic(rawStatistic);
            if (statistic == null) {
                warnings.add("leaderboard '" + id + "' has an unknown statistic '" + rawStatistic + "', skipped");
                return null;
            }
            jsonKey = "minecraft:" + vanillaKey(statistic);
        } else if (source == SourceType.PLACEHOLDER) {
            placeholder = entry.getString("placeholder", "");
            if (placeholder.isBlank()) {
                warnings.add("leaderboard '" + id + "' has no placeholder configured, skipped");
                return null;
            }
            if (!placeholderApiPresent) {
                warnings.add("leaderboard '" + id + "' needs PlaceholderAPI, which is not installed, skipped");
                return null;
            }
        }
        ValueFormat format = ValueFormat.parse(entry.getString("format", "number"));
        if (format == null) {
            warnings.add("leaderboard '" + id + "' has an invalid format '"
                    + entry.getString("format") + "', using 'number'");
            format = ValueFormat.NUMBER;
        }
        SortOrder sortOrder = SortOrder.parse(entry.getString("sorting", "descending"));
        if (sortOrder == null) {
            warnings.add("leaderboard '" + id + "' has an invalid sorting '"
                    + entry.getString("sorting") + "' (ascending or descending), using 'descending'");
            sortOrder = SortOrder.DESCENDING;
        }
        int durationSeconds = entry.getInt("duration-seconds", 15);
        if (durationSeconds < 3) {
            warnings.add("leaderboard '" + id + "' duration-seconds must be at least 3, clamped");
            durationSeconds = 3;
        }
        return new LeaderboardType(
                id,
                source,
                statistic,
                jsonKey,
                placeholder,
                format,
                sortOrder,
                entry.getDouble("minimum-value", 1),
                entry.getString("title", id.toUpperCase(Locale.ROOT)),
                entry.getString("title-color", "#ffffff"),
                entry.getString("value-color", "#FFD263"),
                entry.getString("icon", ""),
                Math.max(1, entry.getInt("icon-frames", 1)),
                entry.getLong("icon-frame-period", 72000L),
                Math.max(1, entry.getInt("icon-frame-pad", 1)),
                Math.max(0, entry.getInt("icon-width", 12)),
                durationSeconds * 20,
                entry.getDouble("progress-bar-length", 0),
                entry.getDouble("progress-bar-width", 0)
        );
    }

    private static Statistic resolveStatistic(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "");
        if (normalized.isEmpty()) {
            return null;
        }
        Statistic alias = VANILLA_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }
        for (Statistic statistic : Statistic.values()) {
            if (statistic.getType() != Statistic.Type.UNTYPED) {
                continue;
            }
            if (statistic.getKey().getKey().equals(normalized)
                    || statistic.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return statistic;
            }
        }
        return null;
    }

    private static String vanillaKey(Statistic statistic) {
        for (Map.Entry<String, Statistic> alias : VANILLA_ALIASES.entrySet()) {
            if (alias.getValue() == statistic) {
                return alias.getKey();
            }
        }
        return statistic.getKey().getKey();
    }

    private static Map<String, Statistic> buildVanillaAliases() {
        Map<String, String> names = Map.ofEntries(
                Map.entry("play_time", "PLAY_ONE_MINUTE"),
                Map.entry("eat_cake_slice", "CAKE_SLICES_EATEN"),
                Map.entry("clean_armor", "ARMOR_CLEANED"),
                Map.entry("clean_banner", "BANNER_CLEANED"),
                Map.entry("clean_shulker_box", "SHULKER_BOX_CLEANED"),
                Map.entry("inspect_dropper", "DROPPER_INSPECTED"),
                Map.entry("inspect_hopper", "HOPPER_INSPECTED"),
                Map.entry("inspect_dispenser", "DISPENSER_INSPECTED"),
                Map.entry("play_noteblock", "NOTEBLOCK_PLAYED"),
                Map.entry("tune_noteblock", "NOTEBLOCK_TUNED"),
                Map.entry("pot_flower", "FLOWER_POTTED"),
                Map.entry("trigger_trapped_chest", "TRAPPED_CHEST_TRIGGERED"),
                Map.entry("open_enderchest", "ENDERCHEST_OPENED"),
                Map.entry("fill_cauldron", "CAULDRON_FILLED"),
                Map.entry("use_cauldron", "CAULDRON_USED"),
                Map.entry("open_chest", "CHEST_OPENED"),
                Map.entry("open_shulker_box", "SHULKER_BOX_OPENED")
        );
        Map<String, Statistic> aliases = new HashMap<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            try {
                aliases.put(entry.getKey(), Statistic.valueOf(entry.getValue()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Map.copyOf(aliases);
    }

    public LeaderboardType get(String id) {
        return id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<LeaderboardType> all() {
        return types.values();
    }

    public Set<String> ids() {
        return types.keySet();
    }

    public List<String> warnings() {
        return warnings;
    }
}
