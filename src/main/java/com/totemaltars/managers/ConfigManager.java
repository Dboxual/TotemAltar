/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 */
package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigManager {
    private final TotemAltars plugin;
    private final Map<String, String> mobAffinityCache = new HashMap<String, String>();

    public ConfigManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.buildMobAffinityCache();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.buildMobAffinityCache();
    }

    public double getShardDropChance() {
        return this.plugin.getConfig().getDouble("shard.drop-chance", 0.01);
    }

    public String getShardName() {
        return this.plugin.getConfig().getString("shard.item-name", "&dTotem Shard");
    }

    public String getShardMorphSound() {
        return this.plugin.getConfig().getString("shard.morph-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public String getShardMorphParticle() {
        return this.plugin.getConfig().getString("shard.morph-particle", "TOTEM_OF_UNDYING");
    }

    public int getAffinityThreshold() {
        return this.plugin.getConfig().getInt("affinity.threshold", 10);
    }

    public String getAffinityTypeForMob(String entityTypeName) {
        return this.mobAffinityCache.get(entityTypeName.toUpperCase());
    }

    public String getAltarName() {
        return this.plugin.getConfig().getString("altar.item-name", "&5Totem Altar");
    }

    public List<String> getAltarRecipeShape() {
        return this.plugin.getConfig().getStringList("altar.recipe.shape");
    }

    public Map<Character, Material> getAltarRecipeIngredients() {
        ConfigurationSection sec = this.plugin.getConfig().getConfigurationSection("altar.recipe.ingredients");
        HashMap<Character, Material> result = new HashMap<Character, Material>();
        if (sec == null) {
            return result;
        }
        for (String key : sec.getKeys(false)) {
            if (key.length() != 1) continue;
            String matName = sec.getString(key, "");
            try {
                result.put(Character.valueOf(key.charAt(0)), Material.valueOf((String)matName.toUpperCase()));
            }
            catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Unknown material in altar recipe: " + matName);
            }
        }
        return result;
    }

    public boolean isHologramEnabled() {
        return this.plugin.getConfig().getBoolean("altar.hologram.enabled", true);
    }

    public String getHologramInactiveText() {
        return this.plugin.getConfig().getString("altar.hologram.inactive-text", "&c&l\u2726 BROKEN ALTAR \u2726");
    }

    public String getHologramActiveText() {
        return this.plugin.getConfig().getString("altar.hologram.active-text", "&b&l\u2726 INSERT SHARD \u2726");
    }

    public String getHologramRitualTitle() {
        return this.plugin.getConfig().getString("altar.hologram.ritual-title", "&5&lRITUAL");
    }

    public String getHologramRitualCounterFormat() {
        return this.plugin.getConfig().getString("altar.hologram.ritual-counter-format", "&dSouls: {current} / {required}");
    }

    public String getHologramAwaitingTotemText() {
        return this.plugin.getConfig().getString("altar.hologram.awaiting-totem-text", "&e&l⚡ INSERT TOTEM ⚡");
    }

    public String getHologramTotemReadyText() {
        return this.plugin.getConfig().getString("altar.hologram.totem-ready-text", "&a&l✦ CLAIM YOUR TOTEM ✦");
    }

    public double getHologramHeight() {
        return this.plugin.getConfig().getDouble("altar.hologram.height", 2.5);
    }

    public boolean isPlacementEffectEnabled() {
        return this.plugin.getConfig().getBoolean("altar.placement-effect.enabled", true);
    }

    public String getPlacementEffectSound() {
        return this.plugin.getConfig().getString("altar.placement-effect.sound", "ENTITY_EVOKER_CAST_SPELL");
    }

    public String getPlacementEffectParticle() {
        return this.plugin.getConfig().getString("altar.placement-effect.particle", "PORTAL");
    }

    public boolean isIdleParticlesEnabled() {
        return this.plugin.getConfig().getBoolean("altar.idle-particles.enabled", true);
    }

    public String getIdleParticle() {
        return this.plugin.getConfig().getString("altar.idle-particles.particle", "WITCH");
    }

    public long getIdleParticleIntervalTicks() {
        return this.plugin.getConfig().getLong("altar.idle-particles.interval-ticks", 40L);
    }

    public int getRitualRequiredKills() {
        return this.plugin.getConfig().getInt("ritual.required-kills", 25);
    }

    public int getRitualMaxAliveMobs() {
        return this.plugin.getConfig().getInt("ritual.max-alive-mobs", 6);
    }

    public long getRitualSpawnIntervalTicks() {
        return this.plugin.getConfig().getLong("ritual.spawn-interval-ticks", 60L);
    }

    public int getRitualSpawnRadiusMin() {
        return this.plugin.getConfig().getInt("ritual.spawn-radius-min", 3);
    }

    public int getRitualSpawnRadiusMax() {
        return this.plugin.getConfig().getInt("ritual.spawn-radius-max", 7);
    }

    public boolean isSpawnLightningEffectEnabled() {
        return this.plugin.getConfig().getBoolean("ritual.spawn-lightning-effect-enabled", true);
    }

    public int getRitualBroadcastRadius() {
        return this.plugin.getConfig().getInt("ritual.broadcast-radius", 30);
    }

    public List<String> getRitualMobPool() {
        List pool = this.plugin.getConfig().getStringList("ritual.mob-pool");
        if (pool.isEmpty()) {
            return List.of("ZOMBIE", "HUSK", "SKELETON", "BLAZE", "VEX");
        }
        return pool;
    }

    public boolean isVexEnabled() {
        return this.plugin.getConfig().getBoolean("ritual.vex.enabled", true);
    }

    public double getVexSpawnChance() {
        return this.plugin.getConfig().getDouble("ritual.vex.spawn-chance", 0.25);
    }

    public int getMaxActiveVexes() {
        return this.plugin.getConfig().getInt("ritual.vex.max-active", 2);
    }

    public boolean isRitualParticlesEnabled() {
        return this.plugin.getConfig().getBoolean("ritual.particles.enabled", true);
    }

    public String getRitualParticle() {
        return this.plugin.getConfig().getString("ritual.particles.particle", "SOUL_FIRE_FLAME");
    }

    public boolean isGuideBookEnabled() {
        return this.plugin.getConfig().getBoolean("guide-book.enabled", true);
    }

    public String getGuideBookSound() {
        return this.plugin.getConfig().getString("guide-book.sound", "ITEM_BOOK_PAGE_TURN");
    }

    public long getGlobalCooldown() {
        return this.plugin.getConfig().getLong("global-cooldown", 15L);
    }

    public double getBlastRadius() {
        return this.plugin.getConfig().getDouble("abilities.blast.radius", 8.0);
    }

    public double getBlastKnockback() {
        return this.plugin.getConfig().getDouble("abilities.blast.knockback-strength", 3.0);
    }

    public int getShadowDuration() {
        return this.plugin.getConfig().getInt("abilities.shadow.duration-seconds", 15);
    }

    public int getStormMinDistance() {
        return this.plugin.getConfig().getInt("abilities.storm.min-distance", 25);
    }

    public int getStormMaxDistance() {
        return this.plugin.getConfig().getInt("abilities.storm.max-distance", 35);
    }

    public int getStormMaxAttempts() {
        return this.plugin.getConfig().getInt("abilities.storm.max-attempts", 50);
    }

    public double getSwapRange() {
        return this.plugin.getConfig().getDouble("abilities.swap.range", 25.0);
    }

    public double getBedrockRelicSpawnChance() {
        return this.plugin.getConfig().getDouble("bedrock-relic.spawn-chance", 0.03);
    }

    public int getBedrockBreakSeconds() {
        return this.plugin.getConfig().getInt("bedrock-relic.break-seconds", 5);
    }

    public int getBedrockBindingTicks() {
        return this.plugin.getConfig().getInt("bedrock-relic.binding-ticks", 100);
    }

    public int getBedrockDurabilityCost() {
        return this.plugin.getConfig().getInt("bedrock-relic.durability-cost", 100);
    }

    public boolean isLegacyCleanupEnabled() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.enabled", true);
    }

    public boolean isLegacyCleanupOnEnableLoadedContainers() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.on-enable-loaded-containers", true);
    }

    public boolean isLegacyCleanupOnPlayerJoin() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.on-player-join", true);
    }

    public boolean isLegacyCleanupOnChunkLoad() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.on-chunk-load", true);
    }

    public boolean isLegacyCleanupScanShulkerContents() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.scan-shulker-contents", true);
    }

    public boolean isLegacyCleanupLogRemovals() {
        return this.plugin.getConfig().getBoolean("legacy-cleanup.log-removals", true);
    }

    public String getItemName(String key) {
        return this.plugin.getConfig().getString("item-names." + key, key);
    }

    public String getMessage(String key) {
        return this.plugin.getConfig().getString("messages." + key, key);
    }

    private void buildMobAffinityCache() {
        this.mobAffinityCache.clear();
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("affinity.mob-types");
        if (section == null) {
            return;
        }
        for (String affinityType : section.getKeys(false)) {
            for (String mob : section.getStringList(affinityType)) {
                this.mobAffinityCache.put(mob.toUpperCase(), affinityType.toLowerCase());
            }
        }
    }
}
