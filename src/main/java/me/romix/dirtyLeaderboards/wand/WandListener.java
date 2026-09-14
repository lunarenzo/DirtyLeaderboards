package me.romix.dirtyLeaderboards.wand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class WandListener implements Listener {

    private final WandManager wandManager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public WandListener(WandManager wandManager) {
        this.wandManager = wandManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!wandManager.isWand(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        BlockFace face = event.getBlockFace();

        if (action == Action.LEFT_CLICK_BLOCK) {
            wandManager.setPos1(player, block.getLocation(), face);
            sendActionBar(player, "<green>Pos1 set to (<x>, <y>, <z>)</green>", block);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        } else {
            wandManager.setPos2(player, block.getLocation(), face);
            sendActionBar(player, "<purple>Pos2 set to (<x>, <y>, <z>)</purple>", block);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        WandManager.WandSelection selection = wandManager.getSelection(player.getUniqueId());
        if (selection != null && selection.isComplete()) {
            WandManager.SelectionResult result = wandManager.calculateWall(selection);
            if (result != null) {
                Component msg = mini.deserialize("<gold>Selection complete! Dimensions: <white><w>x<h></white> blocks. Run <yellow>/dlb create <id></yellow> or <yellow>/dlb setwall <id></yellow></gold>",
                        Placeholder.unparsed("w", String.format("%.1f", result.width())),
                        Placeholder.unparsed("h", String.format("%.1f", result.height())));
                player.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (wandManager.isWand(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wandManager.clearSelection(event.getPlayer().getUniqueId());
    }

    private void sendActionBar(Player player, String template, Block block) {
        Component text = mini.deserialize(template,
                Placeholder.unparsed("x", String.valueOf(block.getX())),
                Placeholder.unparsed("y", String.valueOf(block.getY())),
                Placeholder.unparsed("z", String.valueOf(block.getZ())));
        player.sendActionBar(text);
    }
}
