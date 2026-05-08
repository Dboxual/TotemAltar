package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TotemActivationListener implements Listener {

    private final TotemAltars plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> cooldownTasks = new HashMap<>();

    public TotemActivationListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TOTEM POP DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        ItemStack totemItem = (hand == EquipmentSlot.HAND)
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        String totemType = plugin.getItemUtil().getTotemType(totemItem);
        if (totemType == null) return;

        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), "__global__")) {
            long secs = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), "__global__");
            String msg = plugin.getConfigManager().getMessage("global-cooldown-message")
                    .replace("{seconds}", String.valueOf(secs));
            actionBar(player, msg);
            return;
        }

        plugin.getCooldownManager().setCooldown(
                player.getUniqueId(), "__global__",
                plugin.getConfigManager().getGlobalCooldown());

        startCooldownDisplay(player);

        // Read the link ID now — the item is consumed by vanilla after this event.
        UUID guardianLinkId = "guardian".equals(totemType)
                ? plugin.getItemUtil().getGuardianLinkId(totemItem)
                : null;

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> activateAbility(player, totemType, guardianLinkId), 1L);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ABILITY DISPATCH
    // ═══════════════════════════════════════════════════════════════════════════

    private void activateAbility(Player player, String type, UUID guardianLink) {
        if (!player.isOnline()) return;
        switch (type) {
            case "blast"    -> doBlast(player);
            case "shadow"   -> doShadow(player);
            case "storm"    -> doStorm(player);
            case "swap"     -> doSwap(player);
            case "guardian" -> doGuardian(player, guardianLink);
        }
    }

    // ─── Blast ────────────────────────────────────────────────────────────────────

    private void doBlast(Player player) {
        double radius   = plugin.getConfigManager().getBlastRadius();
        double strength = plugin.getConfigManager().getBlastKnockback();
        Location center = player.getLocation();

        for (Entity nearby : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (nearby.equals(player)) continue;
            if (!(nearby instanceof LivingEntity)) continue;

            Vector dir = nearby.getLocation().subtract(center).toVector();
            if (dir.lengthSquared() < 0.0001) {
                dir = new Vector(random.nextDouble() * 2 - 1, 0.5, random.nextDouble() * 2 - 1);
            } else {
                dir.normalize();
            }
            dir.setY(Math.max(dir.getY(), 0.25));
            nearby.setVelocity(dir.multiply(strength));
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.FLAME, center, 40, 1.2, 0.4, 1.2, 0.06);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        actionBar(player, plugin.getConfigManager().getMessage("blast-activated"));
    }

    // ─── Shadow ───────────────────────────────────────────────────────────────────

    private void doShadow(Player player) {
        int durationTicks = plugin.getConfigManager().getShadowDuration() * 20;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, durationTicks, 0, false, false, false));
        plugin.getShadowArmorManager().startShadow(player, durationTicks);

        player.getWorld().spawnParticle(Particle.SMOKE,
                player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.04);
        player.getWorld().playSound(player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.6f);

        actionBar(player, plugin.getConfigManager().getMessage("shadow-activated"));
    }

    // ─── Storm ────────────────────────────────────────────────────────────────────

    private void doStorm(Player player) {
        Location safe = findSafeLocation(
                player.getLocation(),
                plugin.getConfigManager().getStormMinDistance(),
                plugin.getConfigManager().getStormMaxDistance(),
                plugin.getConfigManager().getStormMaxAttempts());

        if (safe == null) {
            actionBar(player, plugin.getConfigManager().getMessage("no-safe-location"));
            return;
        }

        player.getWorld().spawnParticle(Particle.PORTAL,
                player.getLocation().add(0, 1, 0), 60, 0.3, 0.7, 0.3, 0.6);
        player.getWorld().playSound(player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);

        player.teleport(safe);

        safe.getWorld().spawnParticle(Particle.PORTAL,
                safe.clone().add(0, 1, 0), 60, 0.3, 0.7, 0.3, 0.6);
        safe.getWorld().playSound(safe,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);

        actionBar(player, plugin.getConfigManager().getMessage("storm-activated"));
    }

    private Location findSafeLocation(Location origin, double minDist, double maxDist, int maxAttempts) {
        World world = origin.getWorld();
        for (int i = 0; i < maxAttempts; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist  = minDist + random.nextDouble() * (maxDist - minDist);
            int x = (int) (origin.getX() + dist * Math.cos(angle));
            int z = (int) (origin.getZ() + dist * Math.sin(angle));

            int surfaceY = world.getHighestBlockYAt(x, z);
            Material below = world.getBlockAt(x, surfaceY,     z).getType();
            Material feet  = world.getBlockAt(x, surfaceY + 1, z).getType();
            Material head  = world.getBlockAt(x, surfaceY + 2, z).getType();

            if (!below.isSolid()) continue;
            if (below == Material.LAVA || below == Material.WATER) continue;
            if (!feet.isAir() || !head.isAir()) continue;

            int landY = surfaceY + 1;
            if (landY <= world.getMinHeight() + 4) continue;
            if (landY >= world.getMaxHeight() - 4) continue;

            return new Location(world, x + 0.5, landY, z + 0.5,
                    origin.getYaw(), origin.getPitch());
        }
        return null;
    }

    // ─── Swap ─────────────────────────────────────────────────────────────────────

    private void doSwap(Player player) {
        double range = plugin.getConfigManager().getSwapRange();
        double rangeSq = range * range;

        Player target = null;
        double minDist = Double.MAX_VALUE;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (other.getGameMode() == GameMode.SPECTATOR) continue;
            double dist = player.getLocation().distanceSquared(other.getLocation());
            if (dist <= rangeSq && dist < minDist) {
                minDist = dist;
                target = other;
            }
        }

        if (target == null) {
            actionBar(player, plugin.getConfigManager().getMessage("swap-no-target"));
            return;
        }

        Location playerLoc = player.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        player.getWorld().spawnParticle(Particle.PORTAL,
                playerLoc.clone().add(0, 1, 0), 50, 0.3, 0.7, 0.3, 0.5);
        target.getWorld().spawnParticle(Particle.PORTAL,
                targetLoc.clone().add(0, 1, 0), 50, 0.3, 0.7, 0.3, 0.5);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        Location playerDest = targetLoc.clone();
        playerDest.setYaw(playerLoc.getYaw());
        playerDest.setPitch(playerLoc.getPitch());

        Location targetDest = playerLoc.clone();
        targetDest.setYaw(targetLoc.getYaw());
        targetDest.setPitch(targetLoc.getPitch());

        player.teleport(playerDest);
        target.teleport(targetDest);

        player.getWorld().spawnParticle(Particle.PORTAL,
                target.getLocation().clone().add(0, 1, 0), 50, 0.3, 0.7, 0.3, 0.5);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);

        actionBar(player, plugin.getConfigManager().getMessage("swap-activated"));
        actionBar(target, "&dYou were swapped by " + player.getName() + "!");
    }

    // ─── Guardian ─────────────────────────────────────────────────────────────────

    private void doGuardian(Player player, UUID linkId) {
        if (linkId == null) {
            actionBar(player, plugin.getConfigManager().getMessage("guardian-not-linked"));
            return;
        }

        // Scan ALL online players for anyone holding a totem with the same link ID.
        // This works correctly even if the totem was traded to a third player.
        Location destination = player.getLocation().clone();
        boolean foundAlly = false;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!removeTotemWithLinkId(other, linkId)) continue;

            foundAlly = true;
            other.teleport(destination);

            player.getWorld().spawnParticle(Particle.PORTAL,
                    destination.clone().add(0, 1, 0), 60, 0.5, 0.7, 0.5, 0.5);
            player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

            String allyMsg = plugin.getConfigManager().getMessage("guardian-ally-teleported")
                    .replace("{player}", player.getName());
            other.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(allyMsg));
        }

        if (!foundAlly) {
            actionBar(player, plugin.getConfigManager().getMessage("guardian-partner-offline"));
            return;
        }

        actionBar(player, plugin.getConfigManager().getMessage("guardian-activated"));
    }

    /**
     * Finds and removes the first Guardian Totem in the player's inventory that carries
     * the given link ID. Returns true if one was removed.
     */
    private boolean removeTotemWithLinkId(Player player, UUID linkId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (!"guardian".equals(plugin.getItemUtil().getTotemType(item))) continue;
            if (!linkId.equals(plugin.getItemUtil().getGuardianLinkId(item))) continue;
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(i, null);
            }
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    private void startCooldownDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        Integer existing = cooldownTasks.remove(playerId);
        if (existing != null) Bukkit.getScheduler().cancelTask(existing);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cooldownTasks.remove(playerId); cancel(); return; }
                if (plugin.getCooldownManager().isOnCooldown(playerId, "__global__")) {
                    long secs = plugin.getCooldownManager().getRemainingSeconds(playerId, "__global__");
                    actionBar(player, plugin.getConfigManager().getMessage("global-cooldown-message")
                            .replace("{seconds}", String.valueOf(secs)));
                } else {
                    cooldownTasks.remove(playerId);
                    cancel();
                }
            }
        };
        cooldownTasks.put(playerId, task.runTaskTimer(plugin, 0L, 20L).getTaskId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Integer taskId = cooldownTasks.remove(playerId);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        plugin.getCooldownManager().remove(playerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void actionBar(Player player, String legacyMsg) {
        player.sendActionBar(
                LegacyComponentSerializer.legacyAmpersand().deserialize(legacyMsg));
    }
}
