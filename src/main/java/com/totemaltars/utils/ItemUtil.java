/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package com.totemaltars.utils;

import com.totemaltars.TotemAltars;
import com.totemaltars.utils.LegacyTotemUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ItemUtil {
    private final TotemAltars plugin;
    public final NamespacedKey SHARD_KEY;
    public final NamespacedKey SHARD_TYPE_KEY;
    public final NamespacedKey SHARD_UUID_KEY;
    public final NamespacedKey ALTAR_KEY;
    private final NamespacedKey AFFINITY_BLAST_KEY;
    private final NamespacedKey AFFINITY_SHADOW_KEY;
    private final NamespacedKey AFFINITY_STORM_KEY;
    private final NamespacedKey AFFINITY_GUARDIAN_KEY;
    private final NamespacedKey AFFINITY_SWAP_KEY;
    public final NamespacedKey ALTAR_ACTIVE_KEY;
    // Bedrock Relic progression keys
    public final NamespacedKey BEDROCK_RELIC_KEY;
    public final NamespacedKey BEDROCK_RELIC_ACTIVATED_KEY;
    public final NamespacedKey BEDROCKER_KEY;
    public static final List<String> AFFINITY_TYPES = List.of("blast", "shadow", "storm", "guardian", "swap");

    public ItemUtil(TotemAltars plugin) {
        this.plugin = plugin;
        this.SHARD_KEY = new NamespacedKey((Plugin)plugin, "totem_shard");
        this.SHARD_TYPE_KEY = new NamespacedKey((Plugin)plugin, "shard_type");
        this.SHARD_UUID_KEY = new NamespacedKey((Plugin)plugin, "shard_uuid");
        this.ALTAR_KEY = new NamespacedKey((Plugin)plugin, "totem_altar");
        this.ALTAR_ACTIVE_KEY = new NamespacedKey((Plugin)plugin, "totem_altar_active");
        this.AFFINITY_BLAST_KEY = new NamespacedKey((Plugin)plugin, "affinity_blast");
        this.AFFINITY_SHADOW_KEY = new NamespacedKey((Plugin)plugin, "affinity_shadow");
        this.AFFINITY_STORM_KEY = new NamespacedKey((Plugin)plugin, "affinity_storm");
        this.AFFINITY_GUARDIAN_KEY = new NamespacedKey((Plugin)plugin, "affinity_guardian");
        this.AFFINITY_SWAP_KEY = new NamespacedKey((Plugin)plugin, "affinity_swap");
        this.BEDROCK_RELIC_KEY = new NamespacedKey((Plugin)plugin, "bedrock_relic");
        this.BEDROCK_RELIC_ACTIVATED_KEY = new NamespacedKey((Plugin)plugin, "bedrock_relic_activated");
        this.BEDROCKER_KEY = new NamespacedKey((Plugin)plugin, "bedrocker");
    }

    public ItemStack createShard() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        String nameRaw = this.plugin.getConfigManager().getShardName();
        meta.displayName(this.legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.SHARD_KEY, PersistentDataType.BYTE, (byte)1);
        pdc.set(this.SHARD_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        for (String type : AFFINITY_TYPES) {
            pdc.set(this.affinityKey(type), PersistentDataType.INTEGER, 0);
        }
        item.setItemMeta(meta);
        this.updateShardLore(item);
        return item;
    }

    public boolean isTotemShard(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(this.SHARD_KEY, PersistentDataType.BYTE);
    }

    public ItemStack createAltar() {
        ItemStack item = new ItemStack(Material.DAMAGED_ANVIL);
        ItemMeta meta = item.getItemMeta();
        String nameRaw = this.plugin.getConfigManager().getAltarName();
        meta.displayName(this.legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(this.noItalic(this.legacy("&7A broken altar. Insert a Wither Skull to repair.")), this.noItalic(this.legacy("&8Place and right-click to begin the ritual."))));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        meta.getPersistentDataContainer().set(this.ALTAR_KEY, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTotemAltar(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(this.ALTAR_KEY, PersistentDataType.BYTE);
    }

    public ItemStack createFinishedAltar() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        String nameRaw = this.plugin.getConfigManager().getAltarName();
        meta.displayName(this.legacy(nameRaw).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            this.noItalic(this.legacy("&7A fully repaired altar. Insert a shard to begin forging.")),
            this.noItalic(this.legacy("&8Pre-restored — no ritual required."))
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.ALTAR_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(this.ALTAR_ACTIVE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPreActivatedAltar(ItemStack item) {
        if (!this.isTotemAltar(item)) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(this.ALTAR_ACTIVE_KEY, PersistentDataType.BYTE);
    }

    // Assigns a unique UUID to this shard if it doesn't already have one.
    // Safe to call multiple times — never overwrites an existing UUID.
    public void ensureShardUuid(ItemStack item) {
        if (!this.isTotemShard(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(this.SHARD_UUID_KEY, PersistentDataType.STRING)) {
            return;
        }
        pdc.set(this.SHARD_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
    }

    // Silently upgrades old Echo Shard-based shards to the new Amethyst Shard material.
    // Called lazily whenever a shard lore or morph update occurs.
    private void migrateToAmethystShard(ItemStack item) {
        if (item.getType() != Material.ECHO_SHARD) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        item.setType(Material.AMETHYST_SHARD);
        if (meta != null) {
            item.setItemMeta(meta);
        }
    }

    public String getShardType(ItemStack item) {
        if (!this.isTotemShard(item)) {
            return null;
        }
        return (String)item.getItemMeta().getPersistentDataContainer().get(this.SHARD_TYPE_KEY, PersistentDataType.STRING);
    }

    public boolean isMorphed(ItemStack item) {
        return this.getShardType(item) != null;
    }

    public void morphShard(ItemStack item, String type) {
        String lore;
        String name;
        if (!this.isTotemShard(item) || this.isMorphed(item)) {
            return;
        }
        this.migrateToAmethystShard(item);
        this.ensureShardUuid(item);
        switch (type.toLowerCase()) {
            case "blast": {
                name = "&cBlast-Touched Shard";
                lore = "&7The shard pulses with destructive energy.";
                break;
            }
            case "shadow": {
                name = "&8Shadow-Touched Shard";
                lore = "&7Darkness gathers within the shard.";
                break;
            }
            case "storm": {
                name = "&bStorm-Touched Shard";
                lore = "&7The shard crackles with unstable movement.";
                break;
            }
            case "guardian": {
                name = "&3Guardian-Touched Shard";
                lore = "&7A steady pulse resonates from within.";
                break;
            }
            case "swap": {
                name = "&eSwap-Touched Shard";
                lore = "&7The shard feels strangely unstable.";
                break;
            }
            default: {
                return;
            }
        }
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.legacy(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(this.legacy(lore).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(this.SHARD_TYPE_KEY, PersistentDataType.STRING, type.toLowerCase());
        item.setItemMeta(meta);
    }

    public void updateShardLore(ItemStack item) {
        if (!this.isTotemShard(item) || this.isMorphed(item)) {
            return;
        }
        this.migrateToAmethystShard(item);
        this.ensureShardUuid(item);
        int threshold = this.plugin.getConfigManager().getAffinityThreshold();
        ArrayList<Component> lore = new ArrayList<Component>();
        lore.add(this.noItalic(this.legacy("&7The shard seems to mimic its surroundings.")));
        lore.add(Component.empty());
        lore.add(this.noItalic(this.legacy("&8Progress:")));
        for (String type : AFFINITY_TYPES) {
            int val = this.getAffinity(item, type);
            lore.add(this.noItalic(this.legacy("&7" + this.capitalize(type) + ": &f" + val + "&7/" + threshold)));
        }
        ItemMeta meta = item.getItemMeta();
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public int getAffinity(ItemStack item, String type) {
        if (!this.isTotemShard(item)) {
            return 0;
        }
        NamespacedKey key = this.affinityKey(type);
        if (key == null) {
            return 0;
        }
        Integer val = (Integer)item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public void setAffinity(ItemStack item, String type, int value) {
        if (!this.isTotemShard(item)) {
            return;
        }
        NamespacedKey key = this.affinityKey(type);
        if (key == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    public void addAffinity(ItemStack item, String type, int amount) {
        this.setAffinity(item, type, this.getAffinity(item, type) + amount);
    }

    public String getTotemType(ItemStack item) {
        return LegacyTotemUtil.getLegacyTotemTypeRaw(item);
    }

    public UUID getGuardianLinkId(ItemStack item) {
        return LegacyTotemUtil.getGuardianLinkId(item);
    }

    public ItemStack createForgedTotem(String type) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        String nameKey = type.toLowerCase() + "-totem";
        String name = this.plugin.getConfigManager().getItemName(nameKey);
        String lore = switch (type.toLowerCase()) {
            case "blast" -> "&7The altar has sealed destruction inside.";
            case "shadow" -> "&7Darkness preserved in undying light.";
            case "storm" -> "&7The storm is ready to carry you.";
            case "swap" -> "&7Two souls, one moment.";
            case "guardian" -> "&7Unlinked. Right-click a player to bond.";
            default -> "&7A totem of unknown origin.";
        };
        meta.displayName(this.legacy(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(this.legacy(lore).decoration(TextDecoration.ITALIC, false)));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        meta.getPersistentDataContainer().set(LegacyTotemUtil.LEGACY_TOTEM_TYPE_KEY, PersistentDataType.STRING, type.toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createLinkedGuardianTotem(UUID linkId, String partnerName) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        String name = this.plugin.getConfigManager().getItemName("guardian-totem");
        meta.displayName(this.legacy(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(this.noItalic(this.legacy("&8&oSome bonds hold even across death.")), this.noItalic(this.legacy("&7On pop: pull your &3linked ally&7 to your location.")), this.noItalic(this.legacy("&3Linked to: &f" + partnerName)), this.noItalic(this.legacy("&8&oSealed with Sentinel Eye"))));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(LegacyTotemUtil.LEGACY_TOTEM_TYPE_KEY, PersistentDataType.STRING, "guardian");
        pdc.set(LegacyTotemUtil.LEGACY_GUARDIAN_LINK_ID_KEY, PersistentDataType.STRING, linkId.toString());
        item.setItemMeta(meta);
        return item;
    }

    public String getShardTypeName(String type) {
        return switch (type.toLowerCase()) {
            case "blast"    -> "&cBlast-Touched Shard";
            case "shadow"   -> "&8Shadow-Touched Shard";
            case "storm"    -> "&bStorm-Touched Shard";
            case "guardian" -> "&3Guardian-Touched Shard";
            case "swap"     -> "&eSwap-Touched Shard";
            default         -> type + " Shard";
        };
    }

    public ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Totem Altar Notes");
        meta.setAuthor("The Altar");
        meta.addPages(
            Component.text(
                "You found a Totem Shard.\n\n" +
                "Shards remember what you fight.\n\n" +
                "Hold one in your offhand and defeat certain mobs to awaken its power."
            ),
            Component.text(
                "Altar Recipe:\n\n" +
                "  O  O  O\n" +
                "  D  E  D\n" +
                "  O  N  O\n\n" +
                "O = Obsidian\n" +
                "D = Diamond\n" +
                "E = Echo Shard\n" +
                "N = Nether Star"
            ),
            Component.text(
                "Repair the Altar:\n\n" +
                "Place it. It starts as a cracked anvil.\n\n" +
                "Right-click and insert a Wither Skull to begin a repair ritual.\n\n" +
                "Defeat the mobs until the altar is restored."
            ),
            Component.text(
                "Forging:\n\n" +
                "1. Insert your awakened shard into the active altar.\n\n" +
                "2. Right-click holding a Totem of Undying.\n\n" +
                "3. Right-click again to claim your forged totem."
            )
        );
        book.setItemMeta(meta);
        return book;
    }

    // ── Bedrock Relic items ───────────────────────────────────────────────────

    public ItemStack createBedrockRelic() {
        ItemStack item = new ItemStack(Material.GILDED_BLACKSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.legacy("&6Bedrock Relic").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            this.noItalic(this.legacy("&7A fragment of the world's foundation.")),
            this.noItalic(this.legacy("&8Dormant. Bring to a Bedrock block to awaken.")),
            this.noItalic(this.legacy("&8Found in the ruins of Bastion Remnants."))
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        meta.getPersistentDataContainer().set(this.BEDROCK_RELIC_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBedrockRelic(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(this.BEDROCK_RELIC_KEY, PersistentDataType.BYTE);
    }

    public boolean isActivatedBedrockRelic(ItemStack item) {
        if (!this.isBedrockRelic(item)) return false;
        return item.getItemMeta().getPersistentDataContainer().has(this.BEDROCK_RELIC_ACTIVATED_KEY, PersistentDataType.BYTE);
    }

    /** Creates the correct relic item to drop when a placed relic block is broken. */
    public ItemStack createRelicDrop(boolean activated) {
        ItemStack item = createBedrockRelic();
        if (activated) activateBedrockRelic(item);
        return item;
    }

    /** Transforms an inactive Bedrock Relic into an Activated Bedrock Relic in-place. */
    public void activateBedrockRelic(ItemStack item) {
        if (!this.isBedrockRelic(item) || this.isActivatedBedrockRelic(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.legacy("&6&lActivated Bedrock Relic").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            this.noItalic(this.legacy("&7The relic pulses with the world's foundation.")),
            this.noItalic(this.legacy("&8Bring to a repaired Totem Altar to bind it.")),
            this.noItalic(this.legacy("&6✦ Awakened ✦"))
        ));
        meta.getPersistentDataContainer().set(this.BEDROCK_RELIC_ACTIVATED_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    /** Applies the BedRocker enchantment to a Netherite Pickaxe in-place. */
    public void applyBedRocker(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) return;
        if (this.hasBedRocker(item)) return;
        ItemMeta meta = item.getItemMeta();
        // Append BedRocker lore line
        List<Component> lore = meta.lore() != null ? new java.util.ArrayList<>(meta.lore()) : new java.util.ArrayList<>();
        lore.add(this.noItalic(this.legacy("&5BedRocker")));
        lore.add(this.noItalic(this.legacy("&8Allows mining Bedrock.")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(this.BEDROCKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public boolean hasBedRocker(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(this.BEDROCKER_KEY, PersistentDataType.BYTE);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private NamespacedKey affinityKey(String type) {
        return switch (type.toLowerCase()) {
            case "blast" -> this.AFFINITY_BLAST_KEY;
            case "shadow" -> this.AFFINITY_SHADOW_KEY;
            case "storm" -> this.AFFINITY_STORM_KEY;
            case "guardian" -> this.AFFINITY_GUARDIAN_KEY;
            case "swap" -> this.AFFINITY_SWAP_KEY;
            default -> null;
        };
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    private Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
