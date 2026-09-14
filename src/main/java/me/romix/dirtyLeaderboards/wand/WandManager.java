package me.romix.dirtyLeaderboards.wand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.romix.dirtyLeaderboards.DirtyLeaderboards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

public final class WandManager {

    private static final long EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final Particle.DustOptions DUST_OPTIONS =
            new Particle.DustOptions(Color.fromRGB(167, 69, 255), 0.8f);

    private final DirtyLeaderboards plugin;
    private final NamespacedKey wandKey;
    private final Map<UUID, WandSelection> selections = new ConcurrentHashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private BukkitTask particleTask;

    public record WandSelection(
            Location pos1,
            Location pos2,
            BlockFace pos1Face,
            BlockFace pos2Face,
            long timestamp
    ) {
        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_MILLIS;
        }
    }

    public record SelectionResult(
            Location location,
            double width,
            double height
    ) {
    }

    public WandManager(DirtyLeaderboards plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "leaderboard_wand");
        startParticleTask();
    }

    public NamespacedKey wandKey() {
        return wandKey;
    }

    public ItemStack createWandItem() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "true");
            meta.displayName(mini.deserialize("<gradient:#A745FF:#cf97ff><bold>Leaderboard Wand</bold></gradient>"));
            meta.lore(java.util.List.of(
                    mini.deserialize("<gray>Left-Click block: <purple>Set Pos1</purple></gray>"),
                    mini.deserialize("<gray>Right-Click block: <purple>Set Pos2</purple></gray>"),
                    mini.deserialize("<dark_gray>Use /dlb create or /dlb setwall after selection</dark_gray>")
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.STRING);
    }

    public void setPos1(Player player, Location loc, BlockFace face) {
        UUID uuid = player.getUniqueId();
        WandSelection current = selections.get(uuid);
        Location pos2 = current != null ? current.pos2() : null;
        BlockFace face2 = current != null ? current.pos2Face() : face;
        selections.put(uuid, new WandSelection(loc, pos2, face, face2, System.currentTimeMillis()));
    }

    public void setPos2(Player player, Location loc, BlockFace face) {
        UUID uuid = player.getUniqueId();
        WandSelection current = selections.get(uuid);
        Location pos1 = current != null ? current.pos1() : null;
        BlockFace face1 = current != null ? current.pos1Face() : face;
        selections.put(uuid, new WandSelection(pos1, loc, face1, face, System.currentTimeMillis()));
    }

    public WandSelection getSelection(UUID uuid) {
        WandSelection sel = selections.get(uuid);
        if (sel == null || sel.isExpired()) {
            selections.remove(uuid);
            return null;
        }
        return sel;
    }

    public void clearSelection(UUID uuid) {
        selections.remove(uuid);
    }

    public SelectionResult calculateWall(WandSelection selection) {
        if (selection == null || !selection.isComplete()) {
            return null;
        }
        Location p1 = selection.pos1();
        Location p2 = selection.pos2();
        if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            return null;
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        double height = (maxY - minY) + 1.0;
        double width;
        float yaw;
        double centerX;
        double centerZ;

        BlockFace face = selection.pos1Face();
        boolean zAligned = face == BlockFace.EAST || face == BlockFace.WEST
                || (maxX == minX && maxZ > minZ);

        if (zAligned) {
            width = (maxZ - minZ) + 1.0;
            centerX = minX + 0.5;
            centerZ = (minZ + maxZ + 1.0) / 2.0;
            yaw = face == BlockFace.WEST ? 90.0f : -90.0f;
        } else {
            width = (maxX - minX) + 1.0;
            centerX = (minX + maxX + 1.0) / 2.0;
            centerZ = minZ + 0.5;
            yaw = face == BlockFace.NORTH ? 180.0f : 0.0f;
        }

        Location center = new Location(p1.getWorld(), centerX, minY, centerZ, yaw, 0.0f);
        return new SelectionResult(center, width, height);
    }

    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!isWand(player.getInventory().getItemInMainHand())) {
                    continue;
                }
                WandSelection sel = getSelection(player.getUniqueId());
                if (sel == null || !sel.isComplete()) {
                    continue;
                }
                renderPreviewParticles(player, sel);
            }
        }, 10L, 10L);
    }

    private void renderPreviewParticles(Player player, WandSelection sel) {
        Location p1 = sel.pos1();
        Location p2 = sel.pos2();
        World world = p1.getWorld();
        if (world == null || !world.equals(p2.getWorld())) {
            return;
        }
        double minX = Math.min(p1.getBlockX(), p2.getBlockX());
        double maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + 1.0;
        double minY = Math.min(p1.getBlockY(), p2.getBlockY());
        double maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + 1.0;
        double minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        double maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + 1.0;

        for (double x = minX; x <= maxX; x += 0.5) {
            player.spawnParticle(Particle.DUST, x, minY, minZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, x, maxY, minZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, x, minY, maxZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, x, maxY, maxZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
        }
        for (double y = minY; y <= maxY; y += 0.5) {
            player.spawnParticle(Particle.DUST, minX, y, minZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, maxX, y, minZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, minX, y, maxZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
            player.spawnParticle(Particle.DUST, maxX, y, maxZ, 1, 0, 0, 0, 0, DUST_OPTIONS);
        }
    }

    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        selections.clear();
    }
}
