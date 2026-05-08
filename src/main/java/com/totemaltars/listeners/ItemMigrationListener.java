package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects custom ingredient items that were created by an older version of the
 * plugin (e.g. when a material change occurred in an update) and replaces them
 * with the current version on player login.
 *
 * All item identification uses PersistentDataContainer tags — this listener
 * never touches items that don't carry a TotemAltars PDC key.
 *
 * Totems (TOTEM_OF_UNDYING) are not migrated here because their material never
 * changes; any lore differences between versions are cosmetic and the item
 * still functions correctly.
 */
public class ItemMigrationListener implements Listener {

    // Maps ingredient PDC type → expected current Material
    private static final Map<String, Material> EXPECTED_MATERIAL = new HashMap<>();

    static {
        EXPECTED_MATERIAL.put("blast",    Material.BONE);
        EXPECTED_MATERIAL.put("shadow",   Material.PALE_OAK_LOG);
        EXPECTED_MATERIAL.put("storm",    Material.CHORUS_FRUIT);   // was ENDER_PEARL in <v1.0.15
        EXPECTED_MATERIAL.put("swap",     Material.ECHO_SHARD);     // was CHORUS_FRUIT in <v1.0.15
        EXPECTED_MATERIAL.put("guardian", Material.ENDER_EYE);      // was PRISMARINE_CRYSTALS in <v1.0.15
    }

    private final TotemAltars plugin;

    public ItemMigrationListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Defer one tick so the player's inventory is fully synced before we touch it
        plugin.getServer().getScheduler().runTask(plugin,
                () -> migrateInventory(event.getPlayer()));
    }

    // ── Migration logic ───────────────────────────────────────────────────────────

    private void migrateInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents(); // all 41 slots (main + armor + off-hand)
        int migrated = 0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String type = plugin.getItemUtil().getIngredientType(item);
            if (type == null) continue; // not a TotemAltars ingredient

            Material expected = EXPECTED_MATERIAL.get(type);
            if (expected == null) {
                plugin.getLogger().warning(
                        "Unknown ingredient type '" + type + "' in " + player.getName()
                        + "'s inventory (slot " + i + ") — cannot migrate. "
                        + "The item still has its PDC tag and will work in the altar.");
                continue;
            }

            if (item.getType() == expected) continue; // already current material

            // Replace with a fresh item of the correct material, preserving stack size
            ItemStack replacement = plugin.getItemUtil().createIngredient(type);
            replacement.setAmount(item.getAmount());
            contents[i] = replacement;
            migrated++;

            plugin.getLogger().info("Migrated ingredient '" + type + "' for " + player.getName()
                    + " (" + item.getType().name() + " → " + expected.name() + ", slot " + i + ")");
        }

        if (migrated > 0) {
            inv.setContents(contents);
            player.sendMessage(Component.text(
                    "[TotemAltars] " + migrated + " ingredient(s) were updated to the current version.",
                    NamedTextColor.GOLD));
        }
    }
}
