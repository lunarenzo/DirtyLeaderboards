package me.romix.dirtyLeaderboards.display;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import me.romix.dirtyLeaderboards.api.LeaderboardRotateEvent;
import me.romix.dirtyLeaderboards.config.DisplayConfig;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardRegistry;
import me.romix.dirtyLeaderboards.leaderboard.LeaderboardType;
import me.romix.dirtyLeaderboards.leaderboard.TopEntry;
import me.romix.dirtyLeaderboards.leaderboard.TopService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class LeaderboardDisplay {

    private static final Color TRANSPARENT = Color.fromARGB(0, 0, 0, 0);
    private static final int SLIDE_TICKS = 11;
    private static final int EXIT_TICKS = 10;
    private static final int EXIT_OFFSET_FROM_END = 14;

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final DisplayConfig config;
    private final LeaderboardRegistry registry;
    private final TopService topService;
    private final RowRenderer renderer;

    private final List<LeaderboardType> rotation = new ArrayList<>();
    private final List<Row> activeRows = new ArrayList<>();

    private BukkitTask task;
    private World world;
    private Location base;
    private BlockDisplay billboardBorder;
    private BlockDisplay billboardBackground;
    private BlockDisplay progressBackground;
    private BlockDisplay progressForeground;
    private TextDisplay title;

    private long tickCounter;
    private long cycleStartTick;
    private int cycleTick;
    private int rotationIndex = -1;
    private int generation;
    private LeaderboardType current;
    private List<TopEntry> entries;
    private int spawnedRows;
    private double titleScale;
    private String titleText = "";
    private Component titleComponent = Component.empty();
    private int typedChars;

    private static final class Row {
        final TextDisplay entity;
        long exitStart;
        int slideProgress;
        int exitProgress = -2;

        Row(TextDisplay entity, long exitStart) {
            this.entity = entity;
            this.exitStart = exitStart;
        }
    }

    public LeaderboardDisplay(Plugin plugin, NamespacedKey markerKey, DisplayConfig config,
                              LeaderboardRegistry registry, TopService topService, RowRenderer renderer) {
        this.plugin = plugin;
        this.markerKey = markerKey;
        this.config = config;
        this.registry = registry;
        this.topService = topService;
        this.renderer = renderer;
    }

    public DisplayConfig config() {
        return config;
    }

    public boolean isRunning() {
        return task != null;
    }

    public String currentLeaderboardId() {
        return current == null ? null : current.id();
    }

    public boolean start() {
        if (isRunning()) {
            return true;
        }
        world = Bukkit.getWorld(config.worldName());
        if (world == null) {
            plugin.getLogger().warning("Display '" + config.id() + "' cannot start: world '"
                    + config.worldName() + "' is not loaded");
            return false;
        }
        rotation.clear();
        for (String id : config.rotation()) {
            LeaderboardType type = registry.get(id);
            if (type == null) {
                plugin.getLogger().warning("Display '" + config.id() + "' rotation references unknown"
                        + " or disabled leaderboard '" + id + "', skipped");
                continue;
            }
            rotation.add(type);
        }
        if (rotation.isEmpty()) {
            plugin.getLogger().warning("Display '" + config.id() + "' cannot start: rotation is empty");
            world = null;
            return false;
        }
        Location origin = config.location(world);
        base = origin.clone().add(origin.getDirection().multiply(1 / 16.0));
        if (config.forceLoadChunks()) {
            forEachChunk(chunk -> world.getChunkAt(chunk[0], chunk[1]).addPluginChunkTicket(plugin));
        }
        cleanupMarkedEntities();
        spawnStaticEntities();
        tickCounter = 0;
        cycleTick = 0;
        rotationIndex = -1;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        return true;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        generation++;
        for (Row row : activeRows) {
            row.entity.remove();
        }
        activeRows.clear();
        removeEntity(billboardBorder);
        removeEntity(billboardBackground);
        removeEntity(progressBackground);
        removeEntity(progressForeground);
        removeEntity(title);
        billboardBorder = null;
        billboardBackground = null;
        progressBackground = null;
        progressForeground = null;
        title = null;
        if (world != null && config.forceLoadChunks()) {
            forEachChunk(chunk -> world.getChunkAt(chunk[0], chunk[1]).removePluginChunkTicket(plugin));
        }
        world = null;
        base = null;
        current = null;
        entries = null;
        rotationIndex = -1;
        cycleTick = 0;
    }

    public String skip() {
        if (!isRunning()) {
            return null;
        }

        for (Row row : activeRows) {
            row.exitStart = Math.min(row.exitStart, tickCounter);
        }
        cycleTick = Math.max(0, exitOffset());
        cycleStartTick = tickCounter - cycleTick;
        return rotation.get((rotationIndex + 1) % rotation.size()).id();
    }

    private void tick() {
        tickCounter++;
        advanceRows();
        if (cycleTick == 0) {
            beginCycle();
        }
        animateTitle();
        animateProgress();
        maybeSpawnRow();
        cycleTick++;
        if (cycleTick >= current.durationTicks()) {
            cycleTick = 0;
        }
    }

    private void beginCycle() {
        rotationIndex = (rotationIndex + 1) % rotation.size();
        current = rotation.get(rotationIndex);
        cycleStartTick = tickCounter;
        entries = null;
        spawnedRows = 0;
        typedChars = 0;
        generation++;
        titleScale = renderer.titleScale(current);
        titleText = " " + current.title();
        titleComponent = renderer.titleBase(current);
        resetTitle();
        resetProgress();
        requestData();
        new LeaderboardRotateEvent(config.id(), current.id()).callEvent();
        if (rotation.size() > 1) {
            topService.prefetch(rotation.get((rotationIndex + 1) % rotation.size()));
        }
    }

    private void requestData() {
        int expected = generation;
        topService.top(current).thenAccept(list -> {
            if (expected == generation && isRunning()) {
                entries = list;
            }
        });
    }

    private void resetTitle() {
        if (invalid(title)) {
            title = spawnDisplay(TextDisplay.class, base.clone().add(0, 2.7, 0), display -> {
                display.setBackgroundColor(TRANSPARENT);
                display.setShadowed(true);
            });
        }
        title.setInterpolationDelay(0);
        title.setInterpolationDuration(0);
        title.setTransformation(transformation(0, 0, 0, 2, 2, 2));
        title.text(titleComponent);
    }

    private void animateTitle() {
        if (invalid(title)) {
            return;
        }
        if (cycleTick == 2) {
            applyTitleTransform((float) (titleScale * 1.4), -0.12f);
        } else if (cycleTick == 6) {
            applyTitleTransform((float) titleScale, 0f);
        }
        if (cycleTick >= 6 && (cycleTick - 6) % 2 == 0 && typedChars < titleText.length()) {
            titleComponent = titleComponent.append(renderer.titleChar(current, titleText.charAt(typedChars)));
            title.text(titleComponent);
            typedChars++;
        }
    }

    private void applyTitleTransform(float scale, float yOffset) {
        title.setTransformation(transformation(0, yOffset, 0, scale, scale, scale));
        title.setInterpolationDelay(0);
        title.setInterpolationDuration(4);
    }

    private void resetProgress() {
        if (!config.progressBar().enabled() || invalid(progressForeground)) {
            return;
        }
        float length = effectiveBarLength();
        float width = effectiveBarWidth();
        progressForeground.setInterpolationDelay(0);
        progressForeground.setInterpolationDuration(0);
        progressForeground.setTransformation(transformation(length / -2f, 0, 0.025f, 0, width, 0.025f));
        if (!invalid(progressBackground)) {
            progressBackground.setTransformation(transformation(length / -2f, 0, 0, length, width, 0.025f));
        }
    }

    private void animateProgress() {
        if (!config.progressBar().enabled() || invalid(progressForeground) || cycleTick < 1) {
            return;
        }
        float length = effectiveBarLength();
        float width = effectiveBarWidth();
        float progress = Math.min(1f, cycleTick / (float) current.durationTicks());
        progressForeground.setTransformation(transformation(length / -2f, 0, 0.025f, progress * length, width, 0.025f));
        progressForeground.setInterpolationDelay(0);
        progressForeground.setInterpolationDuration(3);
    }

    private void maybeSpawnRow() {
        if (entries == null || spawnedRows >= TopService.TOP_SIZE) {
            return;
        }
        long exitStart = cycleStartTick + exitOffset();
        if (tickCounter >= exitStart - EXIT_TICKS - 2) {
            return;
        }
        spawnedRows++;
        int rank = spawnedRows;
        Component text = rank <= entries.size()
                ? renderer.realRow(current, entries.get(rank - 1), rank)
                : renderer.fillerRow(current, rank);
        Location location = base.clone().add(0, 2.4 - rank / 4.0, 0);
        TextDisplay row = spawnDisplay(TextDisplay.class, location, display -> {
            display.setLineWidth(config.style().maxPixelWidth());
            display.setBackgroundColor(TRANSPARENT);
            display.setShadowed(true);
            display.text(text);
            display.setTransformation(transformation(-0.6f, 0, 0, 0.8f, 0.8f, 0.8f));
        });
        activeRows.add(new Row(row, exitStart + (rank - 1)));
    }

    private void advanceRows() {
        Iterator<Row> iterator = activeRows.iterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (invalid(row.entity)) {
                iterator.remove();
                continue;
            }
            if (tickCounter >= row.exitStart) {
                row.exitProgress++;
                applyRowTransform(row, (float) (Easing.inCubic(row.exitProgress / 12.0) * 0.7));
                if (row.exitProgress >= EXIT_TICKS) {
                    row.entity.remove();
                    iterator.remove();
                }
            } else if (row.slideProgress < SLIDE_TICKS) {
                row.slideProgress++;
                applyRowTransform(row,
                        (float) (Easing.outCubic(row.slideProgress / (double) SLIDE_TICKS) * 0.6 - 0.6));
            }
        }
    }

    private void applyRowTransform(Row row, float x) {
        row.entity.setTransformation(transformation(x, 0, 0, 0.8f, 0.8f, 0.8f));
        row.entity.setInterpolationDelay(0);
        row.entity.setInterpolationDuration(1);
    }

    private float effectiveBarLength() {
        if (current != null && current.progressBarLength() > 0) {
            return (float) current.progressBarLength();
        }
        return (float) config.progressBar().length();
    }

    private float effectiveBarWidth() {
        if (current != null && current.progressBarWidth() > 0) {
            return (float) current.progressBarWidth();
        }
        return (float) config.progressBar().width();
    }

    private int exitOffset() {
        return current.durationTicks() - EXIT_OFFSET_FROM_END;
    }

    private void spawnStaticEntities() {
        DisplayConfig.Billboard billboard = config.billboard();
        if (billboard.enabled()) {
            float width = (float) (billboard.width() + billboard.borderThickness() * 2);
            float height = (float) (billboard.height() + billboard.borderThickness() * 2);
            float bottom = (float) (billboard.bottomOffset() - billboard.borderThickness());
            billboardBorder = spawnDisplay(BlockDisplay.class, base, display -> {
                display.setBlock(billboard.border());
                display.setTransformation(
                        transformation(width / -2f, bottom, -0.065f, width, height, 0.03f));
            });
            billboardBackground = spawnDisplay(BlockDisplay.class, base, display -> {
                display.setBlock(billboard.background());
                display.setTransformation(transformation((float) (billboard.width() / -2),
                        (float) billboard.bottomOffset(), -0.05f,
                        (float) billboard.width(), (float) billboard.height(), 0.03f));
            });
        }
        if (config.progressBar().enabled()) {
            float length = (float) config.progressBar().length();
            float width = (float) config.progressBar().width();
            Location barLocation = base.clone().add(0, 2.5, 0);
            progressBackground = spawnDisplay(BlockDisplay.class, barLocation, display -> {
                display.setBlock(config.progressBar().background());
                display.setTransformation(transformation(length / -2f, 0, 0, length, width, 0.025f));
            });
            progressForeground = spawnDisplay(BlockDisplay.class, barLocation, display -> {
                display.setBlock(config.progressBar().foreground());
                display.setTransformation(transformation(length / -2f, 0, 0.025f, 0, width, 0.025f));
            });
        }
    }

    private <T extends Display> T spawnDisplay(Class<T> type, Location location, Consumer<T> setup) {
        return world.spawn(location, type, entity -> {
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, config.id());
            setup.accept(entity);
        });
    }

    private void cleanupMarkedEntities() {
        double radius = Math.max(8, config.billboard().width() + 2);
        for (Entity entity : world.getNearbyEntities(base, radius, radius, radius)) {
            if (entity.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    private void forEachChunk(Consumer<int[]> action) {
        int chunkX = base.getBlockX() >> 4;
        int chunkZ = base.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                action.accept(new int[]{chunkX + dx, chunkZ + dz});
            }
        }
    }

    private boolean invalid(Entity entity) {
        return entity == null || !entity.isValid();
    }

    private void removeEntity(Entity entity) {
        if (entity != null) {
            entity.remove();
        }
    }

    private static Transformation transformation(float tx, float ty, float tz,
                                                 float sx, float sy, float sz) {
        return new Transformation(new Vector3f(tx, ty, tz), new AxisAngle4f(),
                new Vector3f(sx, sy, sz), new AxisAngle4f());
    }
}
