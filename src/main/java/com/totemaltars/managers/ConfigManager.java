package com.totemaltars.managers;

import com.totemaltars.TotemAltars;

/**
 * Thin wrapper around Paper's FileConfiguration so every other class
 * can retrieve typed config values without repeating key strings.
 */
public class ConfigManager {

    private final TotemAltars plugin;

    public ConfigManager(TotemAltars plugin) {
        this.plugin = plugin;
    }

    /** Re-reads config.yml from disk. */
    public void reload() {
        plugin.reloadConfig();
    }

    public int getConfigVersion() {
        return plugin.getConfig().getInt("config-version", 0);
    }

    // ── Drop chances ────────────────────────────────────────────────────────────

    public double getDropChance(String type) {
        return plugin.getConfig().getDouble("drop-chances." + type, 0.10);
    }

    // ── Cooldowns ────────────────────────────────────────────────────────────────

    public long getGlobalCooldown() {
        return plugin.getConfig().getLong("global-cooldown", 30L);
    }

    // ── Blast ────────────────────────────────────────────────────────────────────

    public double getBlastRadius()    { return plugin.getConfig().getDouble("abilities.blast.radius",            8.0); }
    public double getBlastKnockback() { return plugin.getConfig().getDouble("abilities.blast.knockback-strength", 3.0); }

    // ── Shadow ───────────────────────────────────────────────────────────────────

    public int getShadowDuration() { return plugin.getConfig().getInt("abilities.shadow.duration-seconds", 15); }

    // ── Storm ────────────────────────────────────────────────────────────────────

    public double getStormMinDistance()  { return plugin.getConfig().getDouble("abilities.storm.min-distance",  25.0); }
    public double getStormMaxDistance()  { return plugin.getConfig().getDouble("abilities.storm.max-distance",  35.0); }
    public int    getStormMaxAttempts()  { return plugin.getConfig().getInt   ("abilities.storm.max-attempts",   50);  }

    // ── Swap ─────────────────────────────────────────────────────────────────────

    public double getSwapRange() { return plugin.getConfig().getDouble("abilities.swap.range", 25.0); }

    // ── Item names & messages ────────────────────────────────────────────────────

    public String getItemName(String key) {
        return plugin.getConfig().getString("item-names." + key, key);
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }
}
