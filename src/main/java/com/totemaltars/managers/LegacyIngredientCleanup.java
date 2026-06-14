package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import com.totemaltars.utils.LegacyTotemUtil;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LegacyIngredientCleanup implements Listener {
    private static final int MAX_SHULKER_DEPTH = 3;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final TotemAltars plugin;
    private final Map<Material, LegacyIngredientSignature> signatures = new EnumMap<>(Material.class);

    public LegacyIngredientCleanup(TotemAltars plugin) {
        this.plugin = plugin;
        this.signatures.put(Material.BONE, new LegacyIngredientSignature(
                "blast",
                "Volatile Bone",
                List.of("A bone crackling with unstable energy.", "Dropped by Wither Skeletons.", "Bring to a Totem Altar.")));
        this.signatures.put(Material.PALE_OAK_LOG, new LegacyIngredientSignature(
                "shadow",
                "Shadow Heartwood",
                List.of("Wood that pulses with pale darkness.", "Dropped by Creakings.", "Bring to a Totem Altar.")));
        this.signatures.put(Material.CHORUS_FRUIT, new LegacyIngredientSignature(
                "storm",
                "Void Chorus",
                List.of("A chorus fruit twisted by the void's will.", "Dropped by Endermen in storms or warped forests.", "Bring to a Totem Altar.")));
        this.signatures.put(Material.ECHO_SHARD, new LegacyIngredientSignature(
                "swap",
                "Fractured Echo",
                List.of("A shard that remembers where you were.", "Dropped by Shulkers.", "Bring to a Totem Altar.")));
        this.signatures.put(Material.ENDER_EYE, new LegacyIngredientSignature(
                "guardian",
                "Sentinel Eye",
                List.of("An eye that never stops watching over the linked.", "Dropped by Guardians.", "Bring to a Totem Altar.")));
    }

    public void runOnEnableCleanup() {
        if (!this.plugin.getConfigManager().isLegacyCleanupEnabled()) {
            return;
        }
        int removed = this.scanOnlinePlayers("enable:online-players");
        if (this.plugin.getConfigManager().isLegacyCleanupOnEnableLoadedContainers()) {
            removed += this.scanLoadedTileInventories("enable:loaded-containers");
        }
        this.logSummary("on-enable", removed);
    }

    public int scanOnlinePlayers(String context) {
        int removed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            removed += this.scanPlayer(player, context + ":" + player.getName());
        }
        return removed;
    }

    public int scanPlayer(Player player, String context) {
        int removed = 0;
        PlayerInventory inventory = player.getInventory();
        removed += this.scanItemArray(inventory.getStorageContents(), inventory::setStorageContents, context + ":inventory", 0);
        removed += this.scanItemArray(inventory.getArmorContents(), inventory::setArmorContents, context + ":armor", 0);
        ItemStack offhand = inventory.getItemInOffHand();
        ItemStack[] offhandSlot = new ItemStack[]{offhand};
        removed += this.scanItemArray(offhandSlot, contents -> inventory.setItemInOffHand(contents[0]), context + ":offhand", 0);
        removed += this.scanInventory(player.getEnderChest(), context + ":ender-chest", 0);
        return removed;
    }

    public int scanLoadedTileInventories(String context) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                removed += this.scanChunk(chunk, context + ":" + world.getName());
            }
        }
        return removed;
    }

    public int scanChunk(Chunk chunk, String context) {
        int removed = 0;
        for (BlockState state : chunk.getTileEntities()) {
            removed += this.scanBlockState(state, context + "@" + chunk.getX() + "," + chunk.getZ());
        }
        return removed;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.plugin.getConfigManager().isLegacyCleanupEnabled() || !this.plugin.getConfigManager().isLegacyCleanupOnPlayerJoin()) {
            return;
        }
        int removed = this.scanPlayer(event.getPlayer(), "join:" + event.getPlayer().getName());
        this.logSummary("player-join:" + event.getPlayer().getName(), removed);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!this.plugin.getConfigManager().isLegacyCleanupEnabled() || !this.plugin.getConfigManager().isLegacyCleanupOnChunkLoad()) {
            return;
        }
        int removed = this.scanChunk(event.getChunk(), "chunk-load:" + event.getWorld().getName());
        this.logSummary("chunk-load:" + event.getWorld().getName() + ":" + event.getChunk().getX() + "," + event.getChunk().getZ(), removed);
    }

    private int scanBlockState(BlockState state, String context) {
        if (!(state instanceof InventoryHolder holder)) {
            return 0;
        }
        int removed = this.scanInventory(holder.getInventory(), context + ":" + state.getType().name(), 0);
        if (removed > 0) {
            state.update(true, false);
        }
        return removed;
    }

    private int scanInventory(Inventory inventory, String context, int depth) {
        int removed = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            CleanResult result = this.cleanItem(inventory.getItem(slot), context + ":slot-" + slot, depth);
            if (result.changed()) {
                inventory.setItem(slot, result.item());
            }
            removed += result.removed();
        }
        return removed;
    }

    private int scanItemArray(ItemStack[] contents, java.util.function.Consumer<ItemStack[]> setter, String context, int depth) {
        int removed = 0;
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            CleanResult result = this.cleanItem(contents[slot], context + ":slot-" + slot, depth);
            if (result.changed()) {
                contents[slot] = result.item();
                changed = true;
            }
            removed += result.removed();
        }
        if (changed) {
            setter.accept(contents);
        }
        return removed;
    }

    private CleanResult cleanItem(ItemStack item, String context, int depth) {
        if (item == null || item.getType().isAir()) {
            return CleanResult.unchanged(item);
        }
        LegacyIngredientSignature signature = this.matchLegacyIngredient(item);
        if (signature != null) {
            int amount = item.getAmount();
            this.logRemoval(context, signature, amount);
            return new CleanResult(null, true, amount);
        }
        if (this.plugin.getConfigManager().isLegacyCleanupScanShulkerContents() && depth < MAX_SHULKER_DEPTH) {
            CleanResult shulkerResult = this.cleanShulkerContents(item, context, depth + 1);
            if (shulkerResult.changed()) {
                return shulkerResult;
            }
        }
        return CleanResult.unchanged(item);
    }

    private CleanResult cleanShulkerContents(ItemStack item, String context, int depth) {
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof BlockStateMeta meta)) {
            return CleanResult.unchanged(item);
        }
        BlockState blockState = meta.getBlockState();
        if (!(blockState instanceof ShulkerBox shulkerBox)) {
            return CleanResult.unchanged(item);
        }
        int removed = this.scanInventory(shulkerBox.getInventory(), context + ":shulker", depth);
        if (removed <= 0) {
            return CleanResult.unchanged(item);
        }
        meta.setBlockState(shulkerBox);
        item.setItemMeta(meta);
        return new CleanResult(item, true, removed);
    }

    public boolean isLegacyIngredientItem(ItemStack item) {
        return this.matchLegacyIngredient(item) != null;
    }

    private LegacyIngredientSignature matchLegacyIngredient(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        LegacyIngredientSignature signature = this.signatures.get(item.getType());
        if (signature == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String pdcType = meta.getPersistentDataContainer().get(LegacyTotemUtil.LEGACY_INGREDIENT_TYPE_KEY, PersistentDataType.STRING);
        if (pdcType != null && pdcType.equalsIgnoreCase(signature.type())) {
            return signature;
        }
        Component displayName = meta.displayName();
        if (displayName == null || !PLAIN.serialize(displayName).equals(signature.displayName())) {
            return null;
        }
        List<Component> lore = meta.lore();
        if (lore == null || lore.size() != signature.lore().size()) {
            return null;
        }
        for (int i = 0; i < lore.size(); i++) {
            if (!PLAIN.serialize(lore.get(i)).equals(signature.lore().get(i))) {
                return null;
            }
        }
        return signature;
    }

    private void logRemoval(String context, LegacyIngredientSignature signature, int amount) {
        if (!this.plugin.getConfigManager().isLegacyCleanupLogRemovals()) {
            return;
        }
        this.plugin.getLogger().info("[legacy-cleanup] Removed " + amount + "x " + signature.displayName()
                + " (" + signature.type().toLowerCase(Locale.ROOT) + ") from " + context);
    }

    private void logSummary(String context, int removed) {
        if (removed > 0 && this.plugin.getConfigManager().isLegacyCleanupLogRemovals()) {
            this.plugin.getLogger().info("[legacy-cleanup] " + context + " removed " + removed + " legacy ingredient item(s).");
        }
    }

    private record LegacyIngredientSignature(String type, String displayName, List<String> lore) {
    }

    private record CleanResult(ItemStack item, boolean changed, int removed) {
        static CleanResult unchanged(ItemStack item) {
            return new CleanResult(item, false, 0);
        }
    }
}
