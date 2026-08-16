package me.romix.dirtyLeaderboards.config;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public record DisplayConfig(
        String id,
        boolean autoStart,
        boolean forceLoadChunks,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        List<String> rotation,
        Billboard billboard,
        ProgressBar progressBar,
        DisplayStyle style
) {

    public record Billboard(
            boolean enabled,
            BlockData background,
            BlockData border,
            double width,
            double height,
            double bottomOffset,
            double borderThickness
    ) {
    }

    public record ProgressBar(
            boolean enabled,
            BlockData background,
            BlockData foreground,
            double length,
            double width
    ) {
    }

    public Location location(World world) {
        return new Location(world, x, y, z, yaw, 0f);
    }

    public DisplayConfig withLocation(String worldName, double x, double y, double z, float yaw) {
        return new DisplayConfig(id, autoStart, forceLoadChunks, worldName, x, y, z, yaw,
                rotation, billboard, progressBar, style);
    }
}
