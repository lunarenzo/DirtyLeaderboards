package me.romix.dirtyLeaderboards.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

public final class UpdateChecker {
    private static final String API_URL =
            "https://api.modrinth.com/v2/project/dirtyleaderboards/version";
    private static final String PAGE_URL = "https://modrinth.com/plugin/dirtyleaderboards";

    private final DirtyLeaderboards plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Set<String> announcedVersions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean failureLogged = new AtomicBoolean();

    private volatile String availableVersion;
    private BukkitTask task;

    public UpdateChecker(DirtyLeaderboards plugin) {
        this.plugin = plugin;
    }

    public void restart() {
        stop();
        if (!plugin.settings().updateCheckerEnabled()) {
            availableVersion = null;
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::check, 5 * 20L, plugin.settings().updateCheckIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public String availableVersion() {
        return availableVersion;
    }

    public String downloadUrl() {
        return PAGE_URL;
    }

    private void check() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .header("User-Agent", "romix42/DirtyLeaderboards/" + currentVersion())
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logFailure("Modrinth answered with HTTP " + response.statusCode());
                return;
            }
            String latest = pickLatest(response.body());
            if (latest == null) {
                logFailure("Modrinth lists no versions for project 'dirtyleaderboards'");
                return;
            }
            if (isNewer(latest, currentVersion())) {
                availableVersion = latest;
                announce(latest);
            } else {
                availableVersion = null;
            }
        } catch (Exception e) {
            logFailure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String pickLatest(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            return null;
        }
        String fallback = null;
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            JsonElement number = version.get("version_number");
            if (number == null) {
                continue;
            }
            if (fallback == null) {
                fallback = number.getAsString();
            }
            JsonElement type = version.get("version_type");
            if (type != null && "release".equals(type.getAsString())) {
                return number.getAsString();
            }
        }
        return fallback;
    }

    private void announce(String latest) {
        if (!announcedVersions.add(latest)) {
            return;
        }
        CommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(Component.empty());
        console.sendMessage(mini.deserialize(
                "<gradient:#A745FF:#cf97ff><bold>DirtyLeaderboards</bold></gradient>"
                        + " <dark_gray>┃</dark_gray> <#ffd263>A new update is available!"));
        console.sendMessage(Component.empty());
        console.sendMessage(mini.deserialize(
                "     <gray>Version</gray>    <white>v" + currentVersion()
                        + "</white> <dark_gray>→</dark_gray> <#92ff60>v" + latest));
        console.sendMessage(mini.deserialize(
                "     <gray>Download</gray>   <#cf97ff><u>" + PAGE_URL + "</u>"));
        console.sendMessage(Component.empty());
    }

    private String currentVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    private void logFailure(String reason) {
        String message = "Could not check for updates: " + reason;
        if (failureLogged.compareAndSet(false, true)) {
            plugin.getLogger().warning(message);
        } else {
            plugin.getLogger().log(Level.FINE, message);
        }
    }

    static boolean isNewer(String remote, String local) {
        int[] remoteParts = numericParts(remote);
        int[] localParts = numericParts(local);
        for (int i = 0; i < Math.max(remoteParts.length, localParts.length); i++) {
            int a = i < remoteParts.length ? remoteParts[i] : 0;
            int b = i < localParts.length ? localParts[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return isPreRelease(local) && !isPreRelease(remote);
    }

    private static int[] numericParts(String version) {
        String cleaned = version.toLowerCase(Locale.ROOT).trim();
        if (cleaned.startsWith("v")) {
            cleaned = cleaned.substring(1);
        }
        int dash = cleaned.indexOf('-');
        if (dash >= 0) {
            cleaned = cleaned.substring(0, dash);
        }
        String[] tokens = cleaned.split("\\.");
        int[] parts = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                parts[i] = Integer.parseInt(tokens[i].trim());
            } catch (NumberFormatException e) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    private static boolean isPreRelease(String version) {
        return version.indexOf('-') >= 0;
    }
}
