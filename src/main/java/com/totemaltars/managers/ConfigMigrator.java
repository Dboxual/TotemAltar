package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles automatic config.yml migration between plugin versions.
 *
 * Rules:
 *  - Never deletes user-set values.
 *  - Adds missing keys with their bundled defaults.
 *  - Renames keys that changed between versions.
 *  - Bumps config-version after each migration step.
 */
public final class ConfigMigrator {

    public static final int CURRENT_VERSION = 1;

    private ConfigMigrator() {}

    /**
     * Run all pending migrations and save if anything changed.
     * Call this once during onEnable, after saveDefaultConfig().
     */
    public static void migrate(TotemAltars plugin) {
        FileConfiguration config = plugin.getConfig();
        int found = config.getInt("config-version", 0);

        if (found >= CURRENT_VERSION) {
            plugin.getLogger().info("Config is up to date (v" + CURRENT_VERSION + ").");
            return;
        }

        boolean changed = false;

        // Apply each migration step in order
        if (found < 1) {
            changed |= migrateToV1(plugin, config);
        }

        config.set("config-version", CURRENT_VERSION);
        plugin.saveConfig();

        if (changed) {
            plugin.getLogger().info("Config successfully migrated to v" + CURRENT_VERSION
                    + ". Your existing settings have been preserved.");
        }
    }

    // ── v0 → v1 ──────────────────────────────────────────────────────────────────
    // Changes in v1:
    //   - item-names.storm-pearl     renamed → item-names.void-chorus
    //   - item-names.mirrored-shard  renamed → item-names.fractured-echo
    //   - item-names.guardian-crystal renamed → item-names.sentinel-eye
    //   - global-cooldown default corrected to 15
    //   - storm min/max distances corrected to 25/35

    private static boolean migrateToV1(TotemAltars plugin, FileConfiguration config) {
        plugin.getLogger().info("Migrating config from v0 to v1...");
        List<String> renamed = new ArrayList<>();
        List<String> added   = new ArrayList<>();

        // Rename keys that changed in the visual identity redesign
        renameKey(config, "item-names.storm-pearl",       "item-names.void-chorus",    renamed);
        renameKey(config, "item-names.mirrored-shard",    "item-names.fractured-echo", renamed);
        renameKey(config, "item-names.guardian-crystal",  "item-names.sentinel-eye",   renamed);

        // Fill in any missing keys using the bundled defaults
        YamlConfiguration defaults = loadBundledDefaults(plugin);
        if (defaults != null) {
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) continue;
                if (!config.isSet(key)) {
                    config.set(key, defaults.get(key));
                    added.add(key);
                }
            }
        }

        if (!renamed.isEmpty()) {
            plugin.getLogger().info("  Renamed keys: " + String.join(", ", renamed));
        }
        if (!added.isEmpty()) {
            plugin.getLogger().info("  Added missing keys: " + String.join(", ", added));
        }
        if (renamed.isEmpty() && added.isEmpty()) {
            plugin.getLogger().info("  No key changes needed.");
        }

        return !renamed.isEmpty() || !added.isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Moves oldKey → newKey if oldKey exists and newKey does not.
     * Removes oldKey afterwards in all cases so it doesn't linger.
     */
    private static void renameKey(FileConfiguration config,
                                   String oldKey, String newKey,
                                   List<String> log) {
        boolean hasOld = config.isSet(oldKey);
        boolean hasNew = config.isSet(newKey);

        if (hasOld && !hasNew) {
            config.set(newKey, config.get(oldKey));
            config.set(oldKey, null);
            log.add(oldKey + " → " + newKey);
        } else if (hasOld) {
            // newKey already exists (possibly user-set); just remove the orphan
            config.set(oldKey, null);
        }
    }

    /** Loads the config.yml bundled inside the jar as a YamlConfiguration. */
    private static YamlConfiguration loadBundledDefaults(TotemAltars plugin) {
        InputStream stream = plugin.getResource("config.yml");
        if (stream == null) {
            plugin.getLogger().warning("Could not load bundled config.yml for migration defaults.");
            return null;
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
