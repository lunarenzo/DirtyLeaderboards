package me.romix.dirtyLeaderboards.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class StartupBanner {

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final List<String> lines = new ArrayList<>();

    public StartupBanner ok(String text) {
        lines.add("<#92ff60>✔</#92ff60> <gray>" + text);
        return this;
    }

    public StartupBanner warn(String text) {
        lines.add("<#ffd263>•</#ffd263> <gray>" + text);
        return this;
    }

    public StartupBanner error(String text) {
        lines.add("<#ff6060>✘</#ff6060> <gray>" + text);
        return this;
    }

    public void print(JavaPlugin plugin) {
        CommandSender console = Bukkit.getConsoleSender();
        String version = plugin.getPluginMeta().getVersion();
        String authors = String.join(", ", plugin.getPluginMeta().getAuthors());
        console.sendMessage(Component.empty());
        console.sendMessage(mini.deserialize(
                "  <gradient:#A745FF:#cf97ff><bold>DirtyLeaderboards</bold></gradient> <dark_gray>v" + version));
        console.sendMessage(mini.deserialize(
                "  <dark_gray>by <gray>" + authors + "</gray> <dark_gray>•</dark_gray> <gray>"
                        + Bukkit.getName() + " " + Bukkit.getMinecraftVersion()));
        console.sendMessage(Component.empty());
        for (String line : lines) {
            console.sendMessage(mini.deserialize("  " + line));
        }
        console.sendMessage(Component.empty());
    }
}
