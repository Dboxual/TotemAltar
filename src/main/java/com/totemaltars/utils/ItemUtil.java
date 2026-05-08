package com.totemaltars.utils;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtil {

    private final TotemAltars plugin;

    public final NamespacedKey INGREDIENT_KEY;    // value = type string
    public final NamespacedKey TOTEM_KEY;         // value = type string
    public final NamespacedKey GUARDIAN_LINK_ID_KEY; // value = shared link UUID (same on both paired totems)

    public ItemUtil(TotemAltars plugin) {
        this.plugin = plugin;
        INGREDIENT_KEY       = new NamespacedKey(plugin, "ingredient_type");
        TOTEM_KEY            = new NamespacedKey(plugin, "totem_type");
        GUARDIAN_LINK_ID_KEY = new NamespacedKey(plugin, "guardian_link_id");
    }

    // ── Ingredients ───────────────────────────────────────────────────────────────

    public ItemStack createIngredient(String type) {
        Material material;
        String   configKey;
        List<String> loreLines = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "blast" -> {
                material  = Material.BONE;
                configKey = "volatile-bone";
                loreLines.add("&7A bone crackling with unstable energy.");
                loreLines.add("&7Dropped by &cWither Skeletons&7.");
                loreLines.add("&7Bring to a &6Totem Altar&7.");
            }
            case "shadow" -> {
                material  = Material.PALE_OAK_LOG;
                configKey = "shadow-heartwood";
                loreLines.add("&7Wood that pulses with pale darkness.");
                loreLines.add("&7Dropped by &8Creakings&7.");
                loreLines.add("&7Bring to a &6Totem Altar&7.");
            }
            case "storm" -> {
                material  = Material.CHORUS_FRUIT;
                configKey = "void-chorus";
                loreLines.add("&7A chorus fruit twisted by the void's will.");
                loreLines.add("&7Dropped by &dEndermen&7 in storms or warped forests.");
                loreLines.add("&7Bring to a &6Totem Altar&7.");
            }
            case "swap" -> {
                material  = Material.ECHO_SHARD;
                configKey = "fractured-echo";
                loreLines.add("&7A shard that remembers where you were — and where you weren't.");
                loreLines.add("&7Dropped by &eShulkers&7.");
                loreLines.add("&7Bring to a &6Totem Altar&7.");
            }
            case "guardian" -> {
                material  = Material.ENDER_EYE;
                configKey = "sentinel-eye";
                loreLines.add("&7An eye that never stops watching over the linked.");
                loreLines.add("&7Dropped by &3Guardians&7.");
                loreLines.add("&7Bring to a &6Totem Altar&7.");
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();

        String nameRaw = plugin.getConfigManager().getItemName(configKey);
        meta.displayName(legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(loreLines));
        meta.getPersistentDataContainer().set(INGREDIENT_KEY, PersistentDataType.STRING, type.toLowerCase());

        item.setItemMeta(meta);
        return item;
    }

    // ── Custom Totems ─────────────────────────────────────────────────────────────

    public ItemStack createTotem(String type) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta  meta = item.getItemMeta();

        String configKey;
        List<String> loreLines = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "blast" -> {
                configKey = "blast-totem";
                double radius = plugin.getConfigManager().getBlastRadius();
                loreLines.add("&8&oThe bone still crackles with unstable fury.");
                loreLines.add("&7On pop: blast all &cnearby entities&7 away.");
                loreLines.add("&7Radius: &c" + formatDouble(radius) + " blocks &8| &7No block damage.");
                loreLines.add("&8&oForged with Volatile Bone");
            }
            case "shadow" -> {
                configKey = "shadow-totem";
                int secs = plugin.getConfigManager().getShadowDuration();
                loreLines.add("&8&oSome things move better unseen.");
                loreLines.add("&7On pop: &8vanish completely&7 for &8" + secs + " seconds&7.");
                loreLines.add("&8Armor hidden &7during invisibility.");
                loreLines.add("&8&oForged with Shadow Heartwood");
            }
            case "storm" -> {
                configKey = "storm-totem";
                int minD = (int) plugin.getConfigManager().getStormMinDistance();
                int maxD = (int) plugin.getConfigManager().getStormMaxDistance();
                loreLines.add("&8&oSpace bends wherever chaos wills.");
                loreLines.add("&7On pop: teleport &d" + minD + "–" + maxD + " blocks&7 away.");
                loreLines.add("&dSafe landing &7guaranteed.");
                loreLines.add("&8&oBound with Void Chorus");
            }
            case "swap" -> {
                configKey = "swap-totem";
                int range = (int) plugin.getConfigManager().getSwapRange();
                loreLines.add("&8&oThe echo knows where you should be standing.");
                loreLines.add("&7On pop: &eswap positions&7 with the nearest player.");
                loreLines.add("&7Range: &e" + range + " blocks&7.");
                loreLines.add("&8&oCrafted using Fractured Echo");
            }
            case "guardian" -> {
                configKey = "guardian-totem";
                loreLines.add("&8&oSome bonds hold even across death.");
                loreLines.add("&7Right-click a player to create a &3Guardian link&7.");
                loreLines.add("&7On pop: pull your &3linked ally&7 to your location.");
                loreLines.add("&8&oSealed with Sentinel Eye");
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }

        String nameRaw = plugin.getConfigManager().getItemName(configKey);
        meta.displayName(legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(loreLines));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(TOTEM_KEY, PersistentDataType.STRING, type.toLowerCase());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a Guardian Totem paired with a specific link.
     * Both totems in a pair share the same linkId — the pop logic scans by this ID,
     * so it works correctly even if the totem is traded to another player.
     */
    public ItemStack createLinkedGuardianTotem(UUID linkId, String partnerName) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta  meta = item.getItemMeta();

        String nameRaw = plugin.getConfigManager().getItemName("guardian-totem");
        meta.displayName(legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(List.of(
                "&8&oSome bonds hold even across death.",
                "&7On pop: pull your &3linked ally&7 to your location.",
                "&3Linked to: &f" + partnerName,
                "&8&oSealed with Sentinel Eye"
        )));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(TOTEM_KEY,            PersistentDataType.STRING, "guardian");
        meta.getPersistentDataContainer().set(GUARDIAN_LINK_ID_KEY, PersistentDataType.STRING, linkId.toString());

        item.setItemMeta(meta);
        return item;
    }

    // ── Identity helpers ──────────────────────────────────────────────────────────

    public String getIngredientType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(INGREDIENT_KEY, PersistentDataType.STRING);
    }

    public String getTotemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(TOTEM_KEY, PersistentDataType.STRING);
    }

    /** Returns the shared link ID for a linked Guardian Totem, or null if unlinked. */
    public UUID getGuardianLinkId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String val = item.getItemMeta().getPersistentDataContainer()
                        .get(GUARDIAN_LINK_ID_KEY, PersistentDataType.STRING);
        if (val == null) return null;
        try { return UUID.fromString(val); }
        catch (IllegalArgumentException e) { return null; }
    }

    public boolean isVanillaTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;
        return getTotemType(item) == null;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────────

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    private List<Component> buildLore(List<String> lines) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(legacy(line).decoration(TextDecoration.ITALIC, false));
        }
        return components;
    }

    private String formatDouble(double d) {
        return (d == Math.floor(d)) ? String.valueOf((int) d) : String.valueOf(d);
    }
}
