package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.Iterator;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Tracks Bedrock Relic items placed as GILDED_BLACKSTONE blocks.
 * GILDED_BLACKSTONE has no TileState, so placement tracking is handled
 * in BedrockRelicBlockManager (plugin-side YAML storage).
 */
public class BedrockRelicPlacementListener implements Listener {
    private final TotemAltars plugin;

    public BedrockRelicPlacementListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.getItemUtil().isBedrockRelic(item)) return;
        Location loc = event.getBlock().getLocation();
        boolean activated = plugin.getItemUtil().isActivatedBedrockRelic(item);
        plugin.getBedrockRelicBlockManager().register(loc, activated);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!plugin.getBedrockRelicBlockManager().isRelicBlock(loc)) return;

        boolean activated = plugin.getBedrockRelicBlockManager().isActivatedBlock(loc);
        plugin.getBedrockRelicBlockManager().unregister(loc);

        event.setDropItems(false);
        event.setExpToDrop(0);

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        ItemStack drop = plugin.getItemUtil().createRelicDrop(activated);
        loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), drop);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    // Prevent pistons from moving tracked relic blocks (movement would desync storage).
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getBedrockRelicBlockManager().isRelicBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getBedrockRelicBlockManager().isRelicBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleExplosionBlocks(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Location loc = block.getLocation();
            if (!plugin.getBedrockRelicBlockManager().isRelicBlock(loc)) continue;

            boolean activated = plugin.getBedrockRelicBlockManager().isActivatedBlock(loc);
            plugin.getBedrockRelicBlockManager().unregister(loc);

            // Remove from explosion list so vanilla handles neither destruction nor drops for this block.
            it.remove();
            // Destroy manually and drop exactly one relic item.
            block.setType(Material.AIR, false);
            ItemStack drop = plugin.getItemUtil().createRelicDrop(activated);
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), drop);
        }
    }
}
