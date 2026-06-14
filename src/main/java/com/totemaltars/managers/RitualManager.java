/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Vex
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class RitualManager {
    public final NamespacedKey RITUAL_MOB_KEY;
    public final NamespacedKey RITUAL_ALTAR_KEY;
    private final TotemAltars plugin;
    private final Random random = new Random();
    private final Map<String, RitualState> activeRituals = new HashMap<String, RitualState>();

    public RitualManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.RITUAL_MOB_KEY = new NamespacedKey((Plugin)plugin, "ritual_mob");
        this.RITUAL_ALTAR_KEY = new NamespacedKey((Plugin)plugin, "ritual_altar");
    }

    public void startRitual(Location loc, Player owner) {
        String key = this.key(loc);
        if (this.activeRituals.containsKey(key)) {
            return;
        }
        if (this.plugin.getAltarManager().isActive(loc)) {
            return;
        }
        int required = this.plugin.getConfigManager().getRitualRequiredKills();
        BossBar bar = Bukkit.createBossBar((String)("Repairing Altar: 0 / " + required + " Souls"), (BarColor)BarColor.PURPLE, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
        bar.setProgress(0.0);
        bar.addPlayer(owner);
        long intervalTicks = this.plugin.getConfigManager().getRitualSpawnIntervalTicks();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> this.topUpMobs(key), 0L, intervalTicks);
        this.activeRituals.put(key, new RitualState(loc, key, owner.getUniqueId(), required, bar, task));
        this.plugin.getAltarEffectsManager().updateRitualHologram(loc, 0, required);
        this.broadcastNearby(loc, this.plugin.getConfigManager().getMessage("ritual-started"));
    }

    public boolean isRitualActive(Location loc) {
        return this.activeRituals.containsKey(this.key(loc));
    }

    public int getRitualCurrentKills(Location loc) {
        RitualState state = this.activeRituals.get(this.key(loc));
        return state != null ? state.currentKills : -1;
    }

    public int getRitualRequired(Location loc) {
        RitualState state = this.activeRituals.get(this.key(loc));
        return state != null ? state.requiredKills : this.plugin.getConfigManager().getRitualRequiredKills();
    }

    public void onRitualMobKilled(Entity entity) {
        for (Map.Entry<String, RitualState> entry : this.activeRituals.entrySet()) {
            RitualState state = entry.getValue();
            if (!state.spawnedMobIds.remove(entity.getUniqueId())) continue;
            ++state.currentKills;
            double progress = Math.min(1.0, (double)state.currentKills / (double)state.requiredKills);
            state.bossBar.setProgress(progress);
            state.bossBar.setTitle("Repairing Altar: " + state.currentKills + " / " + state.requiredKills + " Souls");
            this.plugin.getAltarEffectsManager().updateRitualHologram(state.altarLoc, state.currentKills, state.requiredKills);
            if (state.currentKills >= state.requiredKills) {
                this.completeRitual(entry.getKey());
            }
            return;
        }
    }

    public void collapseRitual(Location loc) {
        String key = this.key(loc);
        RitualState state = this.activeRituals.remove(key);
        if (state == null) {
            return;
        }
        this.cleanUp(state);
        this.plugin.getAltarEffectsManager().onRitualCollapse(loc);
        this.broadcastNearby(loc, this.plugin.getConfigManager().getMessage("ritual-collapsed"));
    }

    public void collapseRitualsOwnedBy(UUID ownerUuid) {
        this.activeRituals.entrySet().removeIf(entry -> {
            RitualState state = (RitualState)entry.getValue();
            if (!state.ownerUuid.equals(ownerUuid)) {
                return false;
            }
            this.cleanUp(state);
            this.plugin.getAltarEffectsManager().onRitualCollapse(state.altarLoc);
            this.broadcastNearby(state.altarLoc, this.plugin.getConfigManager().getMessage("ritual-collapsed"));
            return true;
        });
    }

    public void shutdown() {
        for (RitualState state : this.activeRituals.values()) {
            state.spawnTask.cancel();
            state.bossBar.removeAll();
            this.removeSpawnedMobs(state);
        }
        this.activeRituals.clear();
    }

    private void topUpMobs(String key) {
        RitualState state = this.activeRituals.get(key);
        if (state == null) {
            return;
        }
        if (state.currentKills >= state.requiredKills) {
            return;
        }
        int maxAlive = this.plugin.getConfigManager().getRitualMaxAliveMobs();
        int aliveCount = state.spawnedMobIds.size();
        int toSpawn = Math.max(0, maxAlive - aliveCount);
        for (int i = 0; i < toSpawn; ++i) {
            this.spawnRitualMob(state);
        }
        if (this.plugin.getConfigManager().isRitualParticlesEnabled()) {
            this.spawnRitualParticles(state);
        }
    }

    private void spawnRitualMob(RitualState state) {
        Vex vex;
        EntityType type;
        List<String> pool = this.plugin.getConfigManager().getRitualMobPool();
        if (pool.isEmpty()) {
            return;
        }
        String typeName = pool.get(this.random.nextInt(pool.size()));
        try {
            type = EntityType.valueOf((String)typeName.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Unknown ritual mob type in config: " + typeName);
            return;
        }
        if (type == EntityType.VEX) {
            if (!this.plugin.getConfigManager().isVexEnabled()) {
                return;
            }
            if (this.random.nextDouble() > this.plugin.getConfigManager().getVexSpawnChance()) {
                return;
            }
            if (this.countActiveVexes(state) >= this.plugin.getConfigManager().getMaxActiveVexes()) {
                return;
            }
        }
        int minR = this.plugin.getConfigManager().getRitualSpawnRadiusMin();
        int maxR = this.plugin.getConfigManager().getRitualSpawnRadiusMax();
        Location spawnLoc = this.randomSpawnLoc(state.altarLoc, minR, maxR);
        Entity entity = state.altarLoc.getWorld().spawnEntity(spawnLoc, type);
        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            return;
        }
        entity.getPersistentDataContainer().set(this.RITUAL_MOB_KEY, PersistentDataType.BYTE, (byte)1);
        entity.getPersistentDataContainer().set(this.RITUAL_ALTAR_KEY, PersistentDataType.STRING, state.altarKey);
        if (entity instanceof Vex && (vex = (Vex)entity).getEquipment() != null) {
            vex.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            vex.getEquipment().setItemInMainHandDropChance(0.0f);
        }
        state.spawnedMobIds.add(entity.getUniqueId());
        if (this.plugin.getConfigManager().isSpawnLightningEffectEnabled()) {
            state.altarLoc.getWorld().strikeLightningEffect(spawnLoc);
        }
    }

    private int countActiveVexes(RitualState state) {
        int count = 0;
        for (UUID id : state.spawnedMobIds) {
            Entity e = Bukkit.getEntity((UUID)id);
            if (!(e instanceof Vex)) continue;
            ++count;
        }
        return count;
    }

    private void spawnRitualParticles(RitualState state) {
        String particleName = this.plugin.getConfigManager().getRitualParticle();
        try {
            Particle particle = Particle.valueOf((String)particleName.toUpperCase());
            Location center = state.altarLoc.clone().add(0.5, 0.5, 0.5);
            state.altarLoc.getWorld().spawnParticle(particle, center, 10, 0.6, 0.5, 0.6, 0.04);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
    }

    private void completeRitual(String key) {
        RitualState state = this.activeRituals.remove(key);
        if (state == null) {
            return;
        }
        this.cleanUp(state);
        this.plugin.getAltarEffectsManager().onRitualComplete(state.altarLoc);
        this.plugin.getAltarManager().setActive(state.altarLoc);
        state.altarLoc.getBlock().setType(Material.ANVIL);
        state.altarLoc.getWorld().strikeLightningEffect(state.altarLoc);
        this.broadcastNearby(state.altarLoc, this.plugin.getConfigManager().getMessage("altar-restored"));
    }

    private void cleanUp(RitualState state) {
        state.spawnTask.cancel();
        state.bossBar.removeAll();
        this.removeSpawnedMobs(state);
    }

    private void removeSpawnedMobs(RitualState state) {
        for (UUID mobId : Set.copyOf(state.spawnedMobIds)) {
            Entity entity = Bukkit.getEntity((UUID)mobId);
            if (entity == null || entity.isDead()) continue;
            entity.remove();
        }
        state.spawnedMobIds.clear();
    }

    private Location randomSpawnLoc(Location center, int minRadius, int maxRadius) {
        double angle = this.random.nextDouble() * 2.0 * Math.PI;
        double radius = (double)minRadius + this.random.nextDouble() * (double)(maxRadius - minRadius);
        int dx = (int)Math.round(Math.cos(angle) * radius);
        int dz = (int)Math.round(Math.sin(angle) * radius);
        int x = center.getBlockX() + dx;
        int z = center.getBlockZ() + dz;
        int y = center.getWorld().getHighestBlockYAt(x, z) + 1;
        return new Location(center.getWorld(), (double)x + 0.5, (double)y, (double)z + 0.5);
    }

    private void broadcastNearby(Location loc, String message) {
        int radius = this.plugin.getConfigManager().getRitualBroadcastRadius();
        loc.getWorld().getNearbyPlayers(loc, (double)radius).forEach(p -> p.sendMessage((Component)LegacyComponentSerializer.legacyAmpersand().deserialize(message)));
    }

    private String key(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        return world + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static class RitualState {
        final Location altarLoc;
        final String altarKey;
        final UUID ownerUuid;
        int currentKills;
        final int requiredKills;
        final Set<UUID> spawnedMobIds = new HashSet<UUID>();
        final BossBar bossBar;
        final BukkitTask spawnTask;

        RitualState(Location altarLoc, String altarKey, UUID ownerUuid, int requiredKills, BossBar bossBar, BukkitTask spawnTask) {
            this.altarLoc = altarLoc;
            this.altarKey = altarKey;
            this.ownerUuid = ownerUuid;
            this.currentKills = 0;
            this.requiredKills = requiredKills;
            this.bossBar = bossBar;
            this.spawnTask = spawnTask;
        }
    }
}

