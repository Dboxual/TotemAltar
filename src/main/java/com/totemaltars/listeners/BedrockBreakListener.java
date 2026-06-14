package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

public class BedrockBreakListener implements Listener {
    private final TotemAltars plugin;
    private final Map<UUID, BreakSession> activeSessions = new HashMap<>();

    // Tick interval for the mining loop (crack animation + validation checks).
    private static final int CHECK_INTERVAL = 5;

    public BedrockBreakListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    // ── Start mining ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BEDROCK) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!plugin.getItemUtil().hasBedRocker(tool)) return;

        if (!hasDurabilityForBedrock(tool)) {
            event.setCancelled(true);
            sendActionBar(player, durabilityMessage());
            return;
        }

        UUID uuid = player.getUniqueId();
        if (activeSessions.containsKey(uuid)) return;

        startBreak(player, block.getLocation());
    }

    // ── Natural cancel: player released click or aimed at a different block ──

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BreakSession session = activeSessions.get(uuid);
        if (session == null) return;

        Block aborted = event.getBlock();
        Location sl = session.blockLoc;
        if (aborted.getX() == sl.getBlockX()
                && aborted.getY() == sl.getBlockY()
                && aborted.getZ() == sl.getBlockZ()
                && aborted.getWorld().getUID().equals(sl.getWorld().getUID())) {
            abortSession(uuid, event.getPlayer(), "&cMining stopped.");
        }
    }

    // ── Cleanup on quit / death ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BreakSession session = activeSessions.remove(uuid);
        if (session != null) session.task.cancel();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        abortSession(event.getEntity().getUniqueId(), event.getEntity(), null);
    }

    // ── Core mining loop ─────────────────────────────────────────────────────

    private void startBreak(Player player, Location blockLoc) {
        UUID uuid = player.getUniqueId();
        int totalSeconds = plugin.getConfigManager().getBedrockBreakSeconds();
        int totalTicks   = totalSeconds * 20;

        sendActionBar(player, "&7Mining bedrock... &f" + totalSeconds + "s");
        player.sendBlockDamage(blockLoc, 0.05f);

        int[] ticksElapsed = { 0 };

        BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer((Plugin) plugin, () -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                BreakSession s = activeSessions.remove(uuid);
                if (s != null) s.task.cancel();
                return;
            }

            // Tool still in hand?
            ItemStack tool = p.getInventory().getItemInMainHand();
            if (!plugin.getItemUtil().hasBedRocker(tool)) {
                abortSession(uuid, p, "&cMining interrupted — wrong tool.");
                return;
            }

            // Still in range? (7 blocks)
            if (p.getLocation().distanceSquared(blockLoc.clone().add(0.5, 0.5, 0.5)) > 49.0) {
                abortSession(uuid, p, "&cMining interrupted — moved too far.");
                return;
            }

            // Block still bedrock (another player may have removed it)?
            if (blockLoc.getBlock().getType() != Material.BEDROCK) {
                BreakSession s = activeSessions.remove(uuid);
                if (s != null) s.task.cancel();
                return;
            }

            // Belt-and-suspenders gaze check: player must still look at the exact block.
            // BlockDamageAbortEvent handles most cases; raytrace catches any edge cases.
            RayTraceResult ray = p.rayTraceBlocks(6.0);
            Block gazed = (ray != null) ? ray.getHitBlock() : null;
            if (gazed == null
                    || gazed.getX() != blockLoc.getBlockX()
                    || gazed.getY() != blockLoc.getBlockY()
                    || gazed.getZ() != blockLoc.getBlockZ()
                    || !gazed.getWorld().getUID().equals(blockLoc.getWorld().getUID())) {
                abortSession(uuid, p, "&cMining interrupted — look at the block.");
                return;
            }

            ticksElapsed[0] += CHECK_INTERVAL;
            float progress = (float) ticksElapsed[0] / totalTicks;

            // Break threshold reached
            if (ticksElapsed[0] >= totalTicks) {
                BreakSession s = activeSessions.remove(uuid);
                if (s != null) s.task.cancel();
                if (!hasDurabilityForBedrock(tool)) {
                    sendActionBar(p, durabilityMessage());
                    return;
                }
                applyBedrockDurabilityCost(p, tool);
                blockLoc.getBlock().setType(Material.AIR, false);
                sendActionBar(p, "&6Bedrock shattered.");
                Location center = blockLoc.clone().add(0.5, 0.5, 0.5);
                try { blockLoc.getWorld().strikeLightningEffect(center); } catch (Exception ignored) {}
                try {
                    Sound sound = Sound.valueOf("ENTITY_GENERIC_EXPLODE");
                    blockLoc.getWorld().playSound(center, sound, 1.2f, 0.5f);
                } catch (Exception ignored) {}
                try {
                    Particle particle = Particle.valueOf("SMOKE");
                    blockLoc.getWorld().spawnParticle(particle, center, 40, 0.4, 0.4, 0.4, 0.08);
                } catch (Exception ignored) {}
                return;
            }

            // Update crack animation (10 visual stages mapped via float 0–1)
            p.sendBlockDamage(blockLoc, Math.min(progress, 0.999f));

            // Action bar countdown — update once per second
            if (ticksElapsed[0] % 20 < CHECK_INTERVAL) {
                int secsLeft = Math.max(0, totalSeconds - ticksElapsed[0] / 20);
                sendActionBar(p, "&7Mining bedrock... &f" + secsLeft + "s");
            }

            // Crack audio feedback
            try {
                Sound sound = Sound.valueOf("BLOCK_STONE_HIT");
                blockLoc.getWorld().playSound(blockLoc.clone().add(0.5, 0.5, 0.5), sound, 0.4f, 0.7f);
            } catch (Exception ignored) {}

        }, CHECK_INTERVAL, CHECK_INTERVAL);

        activeSessions.put(uuid, new BreakSession(blockLoc, task));
    }

    // ── Abort: cancel task and visually reset the crack on the client ─────────

    private void abortSession(UUID uuid, Player player, String message) {
        BreakSession session = activeSessions.remove(uuid);
        if (session == null) return;
        session.task.cancel();
        if (player != null) {
            try {
                player.sendBlockChange(session.blockLoc, session.blockLoc.getBlock().getBlockData());
            } catch (Exception ignored) {}
            if (message != null) sendActionBar(player, message);
        }
    }

    // ── Durability helpers ────────────────────────────────────────────────────

    private boolean hasDurabilityForBedrock(ItemStack tool) {
        int cost = plugin.getConfigManager().getBedrockDurabilityCost();
        if (cost <= 0) return true;
        if (tool == null || !tool.hasItemMeta()) return false;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return true;
        int maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) return true;
        return maxDurability - damageable.getDamage() >= cost;
    }

    private void applyBedrockDurabilityCost(Player player, ItemStack tool) {
        int cost = plugin.getConfigManager().getBedrockDurabilityCost();
        if (cost <= 0 || tool == null || !tool.hasItemMeta()) return;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;
        int maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) return;
        int newDamage = damageable.getDamage() + cost;
        if (newDamage >= maxDurability) {
            player.getInventory().setItem(EquipmentSlot.HAND, null);
            try {
                Sound sound = Sound.valueOf("ENTITY_ITEM_BREAK");
                player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void sendActionBar(Player player, String legacy) {
        player.sendActionBar((Component) LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }

    private String durabilityMessage() {
        return plugin.getConfigManager().getMessage("bedrock-break-not-enough-durability")
            .replace("{cost}", String.valueOf(plugin.getConfigManager().getBedrockDurabilityCost()));
    }

    private static class BreakSession {
        final Location blockLoc;
        final BukkitTask task;

        BreakSession(Location blockLoc, BukkitTask task) {
            this.blockLoc = blockLoc;
            this.task = task;
        }
    }
}
