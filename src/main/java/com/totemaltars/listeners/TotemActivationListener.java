/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityResurrectEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TotemActivationListener
implements Listener {
    private final TotemAltars plugin;
    private final Map<UUID, Integer> cooldownTasks = new HashMap<UUID, Integer>();

    public TotemActivationListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onTotemUse(EntityResurrectEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Player)) {
            return;
        }
        Player player = (Player)livingEntity;
        if (event.isCancelled()) {
            return;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        ItemStack totemItem = null;
        if (offhand.getType() == Material.TOTEM_OF_UNDYING) {
            totemItem = offhand;
        } else if (mainhand.getType() == Material.TOTEM_OF_UNDYING) {
            totemItem = mainhand;
        }
        if (totemItem == null) {
            return;
        }
        String totemType = this.plugin.getItemUtil().getTotemType(totemItem);
        if (totemType == null) {
            return;
        }
        UUID linkId = "guardian".equals(totemType) ? this.plugin.getItemUtil().getGuardianLinkId(totemItem) : null;
        UUID playerId = player.getUniqueId();
        if (this.plugin.getCooldownManager().isOnCooldown(playerId, "__global__")) {
            long remaining = this.plugin.getCooldownManager().getRemainingSeconds(playerId, "__global__");
            player.sendActionBar(this.legacy(this.plugin.getConfigManager().getMessage("global-cooldown-message").replace("{seconds}", String.valueOf(remaining))));
            return;
        }
        long cooldownSecs = this.plugin.getConfigManager().getGlobalCooldown();
        this.plugin.getCooldownManager().setCooldown(playerId, "__global__", cooldownSecs);
        this.startCooldownDisplay(player, cooldownSecs);
        String type = totemType;
        UUID gLink = linkId;
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            switch (type) {
                case "blast": {
                    this.doBlast(player);
                    break;
                }
                case "shadow": {
                    this.doShadow(player);
                    break;
                }
                case "storm": {
                    this.doStorm(player);
                    break;
                }
                case "swap": {
                    this.doSwap(player);
                    break;
                }
                case "guardian": {
                    this.doGuardian(player, gLink);
                }
            }
        }, 1L);
    }

    private void doBlast(Player player) {
        double radius = this.plugin.getConfigManager().getBlastRadius();
        double knockback = this.plugin.getConfigManager().getBlastKnockback();
        Location loc = player.getLocation();
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity) || e.equals((Object)player)) continue;
            Vector dir = e.getLocation().toVector().subtract(loc.toVector());
            if (dir.lengthSquared() > 0.0) {
                dir.normalize();
            } else {
                dir = new Vector(0, 1, 0);
            }
            e.setVelocity(dir.multiply(knockback));
        }
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 1.0, 1.0, 1.0, 0.0);
        world.spawnParticle(Particle.FLAME, loc, 30, 1.0, 1.0, 1.0, 0.1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        this.sendMessage(player, this.plugin.getConfigManager().getMessage("blast-activated"));
    }

    private void doShadow(Player player) {
        int durationTicks = this.plugin.getConfigManager().getShadowDuration() * 20;
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false, false));
        this.plugin.getShadowArmorManager().startShadow(player, durationTicks);
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        this.sendMessage(player, this.plugin.getConfigManager().getMessage("shadow-activated"));
    }

    private void doStorm(Player player) {
        int minDist = this.plugin.getConfigManager().getStormMinDistance();
        int maxDist = this.plugin.getConfigManager().getStormMaxDistance();
        int maxAttempts = this.plugin.getConfigManager().getStormMaxAttempts();
        Location target = this.findSafeLocation(player.getLocation(), minDist, maxDist, maxAttempts);
        if (target == null) {
            this.sendMessage(player, this.plugin.getConfigManager().getMessage("no-safe-location"));
            return;
        }
        Location from = player.getLocation().clone();
        player.teleport(target);
        from.getWorld().spawnParticle(Particle.PORTAL, from, 50, 0.5, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.PORTAL, target, 50, 0.5, 0.5, 0.5, 0.5);
        target.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        this.sendMessage(player, this.plugin.getConfigManager().getMessage("storm-activated"));
    }

    private void doSwap(Player player) {
        double range = this.plugin.getConfigManager().getSwapRange();
        List<LivingEntity> pool = new java.util.ArrayList<>();
        // Add nearby non-spectator players (excluding self)
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distance(player.getLocation()) > range) continue;
            pool.add(p);
        }
        // Add nearby living mobs (excluding players, armor stands, named entities, dead entities)
        for (LivingEntity e : player.getWorld().getNearbyEntitiesByType(LivingEntity.class, player.getLocation(), range)) {
            if (e instanceof Player) continue;
            if (e instanceof ArmorStand) continue;
            if (e.isDead()) continue;
            if (e.customName() != null) continue;
            if (e.getCustomName() != null) continue;
            pool.add(e);
        }
        if (pool.isEmpty()) {
            this.sendMessage(player, this.plugin.getConfigManager().getMessage("swap-no-target"));
            return;
        }
        LivingEntity target = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        Location fromLoc = player.getLocation().clone();
        Location toLoc = target.getLocation().clone();
        Location playerDest = toLoc.clone();
        playerDest.setYaw(fromLoc.getYaw());
        playerDest.setPitch(fromLoc.getPitch());
        Location targetDest = fromLoc.clone();
        targetDest.setYaw(toLoc.getYaw());
        targetDest.setPitch(toLoc.getPitch());
        player.teleport(playerDest);
        target.teleport(targetDest);
        fromLoc.getWorld().spawnParticle(Particle.PORTAL, fromLoc, 30, 0.5, 0.5, 0.5, 0.5);
        toLoc.getWorld().spawnParticle(Particle.PORTAL, toLoc, 30, 0.5, 0.5, 0.5, 0.5);
        fromLoc.getWorld().playSound(fromLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        toLoc.getWorld().playSound(toLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        this.sendMessage(player, this.plugin.getConfigManager().getMessage("swap-activated"));
        if (target instanceof Player targetPlayer) {
            targetPlayer.sendActionBar(this.legacy("&dYou were swapped by &f" + player.getName() + "&d!"));
        }
    }

    private void doGuardian(Player player, UUID linkId) {
        if (linkId == null) {
            this.sendMessage(player, this.plugin.getConfigManager().getMessage("guardian-not-linked"));
            return;
        }
        for (Player ally : Bukkit.getOnlinePlayers()) {
            if (ally.equals((Object)player) || !this.removeTotemWithLinkId(ally, linkId)) continue;
            Location dest = player.getLocation().clone();
            dest.setYaw(ally.getLocation().getYaw());
            dest.setPitch(ally.getLocation().getPitch());
            ally.teleport(dest);
            dest.getWorld().spawnParticle(Particle.PORTAL, dest, 50, 0.5, 0.5, 0.5, 0.5);
            dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            this.sendMessage(player, this.plugin.getConfigManager().getMessage("guardian-activated").replace("{player}", ally.getName()));
            this.sendMessage(ally, this.plugin.getConfigManager().getMessage("guardian-ally-teleported").replace("{player}", player.getName()));
            return;
        }
        this.sendMessage(player, this.plugin.getConfigManager().getMessage("guardian-partner-offline"));
    }

    private boolean removeTotemWithLinkId(Player target, UUID linkId) {
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length; ++i) {
            ItemStack item = contents[i];
            if (item == null || !"guardian".equals(this.plugin.getItemUtil().getTotemType(item)) || !linkId.equals(this.plugin.getItemUtil().getGuardianLinkId(item))) continue;
            if (item.getAmount() == 1) {
                target.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - 1);
            }
            return true;
        }
        return false;
    }

    private Location findSafeLocation(Location origin, int minDist, int maxDist, int maxAttempts) {
        World world = origin.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < maxAttempts; ++i) {
            int z;
            double angle = rng.nextDouble() * 2.0 * Math.PI;
            double dist = (double)minDist + rng.nextDouble() * (double)(maxDist - minDist);
            int x = (int)(origin.getX() + Math.cos(angle) * dist);
            int y = world.getHighestBlockYAt(x, z = (int)(origin.getZ() + Math.sin(angle) * dist)) + 1;
            if (y < world.getMinHeight() || y >= world.getMaxHeight() - 1) continue;
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block ground = world.getBlockAt(x, y - 1, z);
            if (!feet.getType().isAir() || !head.getType().isAir() || !ground.getType().isSolid()) continue;
            return new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5, origin.getYaw(), origin.getPitch());
        }
        return null;
    }

    private void startCooldownDisplay(final Player player, long cooldownSecs) {
        final UUID id = player.getUniqueId();
        Integer existing = this.cooldownTasks.remove(id);
        if (existing != null) {
            Bukkit.getScheduler().cancelTask(existing.intValue());
        }
        BukkitRunnable task = new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    TotemActivationListener.this.cooldownTasks.remove(id);
                    return;
                }
                long remaining = TotemActivationListener.this.plugin.getCooldownManager().getRemainingSeconds(id, "__global__");
                if (remaining <= 0L) {
                    this.cancel();
                    TotemActivationListener.this.cooldownTasks.remove(id);
                    return;
                }
                player.sendActionBar(TotemActivationListener.this.legacy(TotemActivationListener.this.plugin.getConfigManager().getMessage("cooldown-display").replace("{seconds}", String.valueOf(remaining))));
            }
        };
        this.cooldownTasks.put(id, task.runTaskTimer((Plugin)this.plugin, 0L, 20L).getTaskId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Integer taskId = this.cooldownTasks.remove(id);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId.intValue());
        }
        this.plugin.getCooldownManager().remove(id);
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(this.legacy(message));
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}

