/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.entity.TextDisplay$TextAlignment
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.world.ChunkLoadEvent
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import com.totemaltars.managers.RitualManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AltarEffectsManager
implements Listener {
    public final NamespacedKey ALTAR_HOLOGRAM_KEY;
    public final NamespacedKey ALTAR_TOTEM_DISPLAY_KEY;
    private final TotemAltars plugin;
    private final Map<String, UUID> activeHolos = new HashMap<String, UUID>();
    private final Map<String, UUID> activeTotemDisplays = new HashMap<String, UUID>();
    private BukkitTask idleTask;

    public AltarEffectsManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.ALTAR_HOLOGRAM_KEY = new NamespacedKey((Plugin)plugin, "altar_hologram");
        this.ALTAR_TOTEM_DISPLAY_KEY = new NamespacedKey((Plugin)plugin, "altar_totem_display");
    }

    public void start() {
        this.startIdleTask();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::restoreHologramsForLoadedChunks, 1L);
    }

    public void shutdown() {
        if (this.idleTask != null) {
            this.idleTask.cancel();
            this.idleTask = null;
        }
    }

    public void onAltarPlaced(Location loc, Player player) {
        this.spawnPlacementEffect(loc, player);
        this.spawnHologram(loc);
    }

    public void onAltarRemoved(Location loc) {
        String key = this.altarKey(loc);
        this.removeHologram(key);
        this.removeTotemDisplay(key);
    }

    public void onShardAccepted(Location loc) {
        this.ensureTotemDisplay(loc);
        this.updateHologramText(loc, this.toLegacyComponent(this.plugin.getConfigManager().getHologramAwaitingTotemText()));
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        try {
            Sound sound = Sound.valueOf("ENTITY_ENDERMAN_TELEPORT");
            loc.getWorld().playSound(center, sound, 1.0f, 1.5f);
        } catch (IllegalArgumentException ignored) {}
    }

    // Called when vanilla Totem of Undying is inserted — plays thunder effects and
    // leaves a persistent Totem of Undying display until the player claims it.
    public void onTotemInserted(Location loc) {
        this.removeTotemDisplay(this.altarKey(loc));
        this.playForgingCompleteEffects(loc);
        this.updateHologramText(loc, this.toLegacyComponent(this.plugin.getConfigManager().getHologramTotemReadyText()));
        this.ensureTotemReadyDisplay(loc);
    }

    // Called after the player claims the forged totem — removes the display and resets the altar hologram.
    public void onForgingComplete(Location loc) {
        this.removeTotemDisplay(this.altarKey(loc));
        this.playClaimEffect(loc);
        this.updateHologramText(loc, this.toLegacyComponent(this.plugin.getConfigManager().getHologramActiveText()));
    }

    public void updateRitualHologram(Location loc, int current, int required) {
        this.updateHologramText(loc, this.buildRitualComponent(current, required));
    }

    public void onRitualComplete(Location loc) {
        this.updateHologramText(loc, this.toLegacyComponent(this.plugin.getConfigManager().getHologramActiveText()));
    }

    public void onRitualCollapse(Location loc) {
        this.updateHologramText(loc, this.toLegacyComponent(this.plugin.getConfigManager().getHologramInactiveText()));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof TextDisplay) {
                String key = (String)entity.getPersistentDataContainer().get(this.ALTAR_HOLOGRAM_KEY, PersistentDataType.STRING);
                if (key != null && !this.plugin.getAltarManager().hasAltarByKey(key)) {
                    entity.remove();
                }
            } else if (entity instanceof ItemDisplay) {
                String key = (String)entity.getPersistentDataContainer().get(this.ALTAR_TOTEM_DISPLAY_KEY, PersistentDataType.STRING);
                if (key != null && !this.plugin.getAltarManager().hasAltarByKey(key)) {
                    entity.remove();
                }
            }
        }
        if (!this.plugin.getConfigManager().isHologramEnabled()) {
            return;
        }
        for (Location loc : this.plugin.getAltarManager().getAllAltarLocations()) {
            if (loc.getWorld() == null || !loc.getWorld().equals((Object)chunk.getWorld()) || loc.getBlockX() >> 4 != chunk.getX() || loc.getBlockZ() >> 4 != chunk.getZ()) continue;
            this.ensureHologram(loc);
            if (this.plugin.getAltarManager().isAwaitingTotem(loc)) {
                this.ensureTotemDisplay(loc);
            } else if (this.plugin.getAltarManager().isTotemReady(loc)) {
                this.ensureTotemReadyDisplay(loc);
            }
        }
    }

    private void startIdleTask() {
        long interval = this.plugin.getConfigManager().getIdleParticleIntervalTicks();
        this.idleTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tickIdleParticles, interval, interval);
    }

    private void tickIdleParticles() {
        Particle particle;
        if (!this.plugin.getConfigManager().isIdleParticlesEnabled()) {
            return;
        }
        String particleName = this.plugin.getConfigManager().getIdleParticle();
        try {
            particle = Particle.valueOf((String)particleName.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return;
        }
        for (Location loc : this.plugin.getAltarManager().getAllAltarLocations()) {
            if (loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 0.8, 0.5), 4, 0.35, 0.3, 0.35, 0.01);
        }
    }

    private void spawnPlacementEffect(Location loc, Player player) {
        if (!this.plugin.getConfigManager().isPlacementEffectEnabled()) {
            return;
        }
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        String soundName = this.plugin.getConfigManager().getPlacementEffectSound();
        try {
            Sound sound = Sound.valueOf((String)soundName.toUpperCase());
            loc.getWorld().playSound(center, sound, 1.2f, 0.75f);
        }
        catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("[effects] Invalid placement sound in config: " + soundName);
        }
        String particleName = this.plugin.getConfigManager().getPlacementEffectParticle();
        try {
            Particle particle = Particle.valueOf((String)particleName.toUpperCase());
            loc.getWorld().spawnParticle(particle, center, 35, 0.5, 0.5, 0.5, 0.12);
        }
        catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("[effects] Invalid placement particle in config: " + particleName);
        }
    }

    private void spawnHologram(Location loc) {
        if (!this.plugin.getConfigManager().isHologramEnabled()) {
            return;
        }
        this.ensureHologram(loc);
    }

    private void ensureHologram(Location loc) {
        this.ensureHologramWithText(loc, this.computeHologramText(loc));
    }

    private void ensureHologramWithText(Location loc, Component text) {
        String key = this.altarKey(loc);
        UUID existing = this.activeHolos.get(key);
        if (existing != null) {
            Entity e = Bukkit.getEntity((UUID)existing);
            if (e instanceof TextDisplay) {
                TextDisplay display = (TextDisplay)e;
                if (!e.isDead()) {
                    display.text(text);
                    return;
                }
            }
            this.activeHolos.remove(key);
        }
        double height = this.plugin.getConfigManager().getHologramHeight();
        Location holoLoc = loc.clone().add(0.5, height, 0.5);
        TextDisplay display = (TextDisplay)loc.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        display.text(text);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        display.setGravity(false);
        display.setPersistent(false);
        display.getPersistentDataContainer().set(this.ALTAR_HOLOGRAM_KEY, PersistentDataType.STRING, key);
        this.activeHolos.put(key, display.getUniqueId());
    }

    private void updateHologramText(Location loc, Component text) {
        String key = this.altarKey(loc);
        UUID uuid = this.activeHolos.get(key);
        if (uuid == null) {
            return;
        }
        Entity entity = Bukkit.getEntity((UUID)uuid);
        if (entity instanceof TextDisplay) {
            TextDisplay display = (TextDisplay)entity;
            if (!entity.isDead()) {
                display.text(text);
            }
        }
    }

    private Component computeHologramText(Location loc) {
        RitualManager rm = this.plugin.getRitualManager();
        if (rm != null && rm.isRitualActive(loc)) {
            return this.buildRitualComponent(rm.getRitualCurrentKills(loc), rm.getRitualRequired(loc));
        }
        if (this.plugin.getAltarManager().isTotemReady(loc)) {
            return this.toLegacyComponent(this.plugin.getConfigManager().getHologramTotemReadyText());
        }
        if (this.plugin.getAltarManager().isAwaitingTotem(loc)) {
            return this.toLegacyComponent(this.plugin.getConfigManager().getHologramAwaitingTotemText());
        }
        if (this.plugin.getAltarManager().isActive(loc)) {
            return this.toLegacyComponent(this.plugin.getConfigManager().getHologramActiveText());
        }
        return this.toLegacyComponent(this.plugin.getConfigManager().getHologramInactiveText());
    }

    private Component buildRitualComponent(int current, int required) {
        String titleRaw = this.plugin.getConfigManager().getHologramRitualTitle();
        String counterRaw = this.plugin.getConfigManager().getHologramRitualCounterFormat().replace("{current}", String.valueOf(current)).replace("{required}", String.valueOf(required));
        return this.toLegacyComponent(titleRaw).append((Component)Component.newline()).append(this.toLegacyComponent(counterRaw));
    }

    private Component toLegacyComponent(String raw) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }

    private void removeHologram(String key) {
        UUID uuid = this.activeHolos.remove(key);
        if (uuid == null) {
            return;
        }
        Entity entity = Bukkit.getEntity((UUID)uuid);
        if (entity != null) {
            entity.remove();
        }
    }

    private void restoreHologramsForLoadedChunks() {
        if (!this.plugin.getConfigManager().isHologramEnabled()) {
            return;
        }
        for (Location loc : this.plugin.getAltarManager().getAllAltarLocations()) {
            if (loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            this.ensureHologram(loc);
            if (this.plugin.getAltarManager().isAwaitingTotem(loc)) {
                this.ensureTotemDisplay(loc);
            } else if (this.plugin.getAltarManager().isTotemReady(loc)) {
                this.ensureTotemReadyDisplay(loc);
            }
        }
    }

    private void ensureTotemDisplay(Location loc) {
        String key = this.altarKey(loc);
        UUID existing = this.activeTotemDisplays.get(key);
        if (existing != null) {
            Entity e = Bukkit.getEntity(existing);
            if (e instanceof ItemDisplay && !e.isDead()) {
                return;
            }
            this.activeTotemDisplays.remove(key);
        }
        // Show the actual typed shard while the altar awaits a Totem of Undying
        String shardType = this.plugin.getAltarManager().getType(loc);
        ItemStack displayItem;
        if (shardType != null) {
            displayItem = this.plugin.getItemUtil().createShard();
            this.plugin.getItemUtil().morphShard(displayItem, shardType);
        } else {
            displayItem = new ItemStack(Material.TOTEM_OF_UNDYING);
        }
        Location displayLoc = loc.clone().add(0.5, 1.4, 0.5);
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(displayLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(displayItem);
        display.setGravity(false);
        display.setPersistent(false);
        display.setBillboard(Display.Billboard.CENTER);
        display.getPersistentDataContainer().set(this.ALTAR_TOTEM_DISPLAY_KEY, PersistentDataType.STRING, key);
        this.activeTotemDisplays.put(key, display.getUniqueId());
    }

    private void ensureTotemReadyDisplay(Location loc) {
        String key = this.altarKey(loc);
        UUID existing = this.activeTotemDisplays.get(key);
        if (existing != null) {
            Entity e = Bukkit.getEntity(existing);
            if (e instanceof ItemDisplay && !e.isDead()) {
                return;
            }
            this.activeTotemDisplays.remove(key);
        }
        Location displayLoc = loc.clone().add(0.5, 1.4, 0.5);
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(displayLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(Material.TOTEM_OF_UNDYING));
        display.setGravity(false);
        display.setPersistent(false);
        display.setBillboard(Display.Billboard.CENTER);
        display.getPersistentDataContainer().set(this.ALTAR_TOTEM_DISPLAY_KEY, PersistentDataType.STRING, key);
        this.activeTotemDisplays.put(key, display.getUniqueId());
    }

    private void playClaimEffect(Location loc) {
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        try {
            Sound sound = Sound.valueOf("ENTITY_ITEM_PICKUP");
            loc.getWorld().playSound(center, sound, 1.0f, 1.2f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void removeTotemDisplay(String key) {
        UUID uuid = this.activeTotemDisplays.remove(key);
        if (uuid == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) {
            entity.remove();
        }
    }

    private void playForgingCompleteEffects(Location loc) {
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        loc.getWorld().strikeLightningEffect(center);
        try {
            Particle particle = Particle.valueOf("TOTEM_OF_UNDYING");
            loc.getWorld().spawnParticle(particle, center, 60, 0.5, 1.0, 0.5, 0.3);
        } catch (IllegalArgumentException ignored) {}
        try {
            Sound sound = Sound.valueOf("BLOCK_BEACON_ACTIVATE");
            loc.getWorld().playSound(center, sound, 1.5f, 0.7f);
        } catch (IllegalArgumentException ignored) {}
    }

    private String altarKey(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        return world + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}

