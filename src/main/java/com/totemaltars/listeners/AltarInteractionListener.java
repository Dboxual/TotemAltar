package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class AltarInteractionListener
implements Listener {
    private final TotemAltars plugin;
    // Repair GUI state (Wither Skull → ritual start) — kept from original flow
    private final Map<UUID, Location> repairGUILocs = new HashMap<>();
    private final Map<UUID, Inventory> repairGUIInvs = new HashMap<>();
    // Lock set: prevents double-processing from rapid clicks during state transitions
    private final Set<String> forgingInProgress = new HashSet<>();

    public AltarInteractionListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main dispatch
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Material mat = block.getType();
        if (mat != Material.DAMAGED_ANVIL && mat != Material.ANVIL) {
            return;
        }
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        if (!this.plugin.getAltarManager().isAltar(loc)) {
            return;
        }
        event.setCancelled(true);

        // Stage 3 — forged totem sitting on altar, waiting to be claimed
        if (this.plugin.getAltarManager().isTotemReady(loc)) {
            this.handleTotemReadyClaim(player, event.getHand(), loc);
            return;
        }
        // Stage 2 — shard inserted, waiting for vanilla Totem of Undying
        if (this.plugin.getAltarManager().isAwaitingTotem(loc)) {
            this.handleTotemClick(player, event.getHand(), loc);
            return;
        }
        // Stage 1 — altar restored, waiting for player to hand-insert a typed shard
        if (this.plugin.getAltarManager().isActive(loc)) {
            this.handleShardInsert(player, event.getHand(), loc);
            return;
        }
        // Stage 0 — altar broken; ritual active is a sub-state
        if (this.plugin.getRitualManager().isRitualActive(loc)) {
            this.msg(player, this.plugin.getConfigManager().getMessage("ritual-already-active"));
            return;
        }
        // Stage 0 — broken altar, no active ritual → open repair GUI (Wither Skull)
        this.openRepairGUI(player, loc);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stage 1 → 2: hand-insert typed shard
    // ─────────────────────────────────────────────────────────────────────────

    private void handleShardInsert(Player player, EquipmentSlot hand, Location altarLoc) {
        if (hand != EquipmentSlot.HAND) {
            return;
        }
        String key = this.altarKey(altarLoc);
        if (this.forgingInProgress.contains(key)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        // getShardType returns null for: null, AIR, non-shard, unmorphed shard
        String shardType = this.plugin.getItemUtil().getShardType(held);
        if (shardType == null) {
            this.msg(player, this.plugin.getConfigManager().getMessage("forge-needs-shard"));
            return;
        }
        this.forgingInProgress.add(key);
        // Consume exactly 1 typed shard
        if (held.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
        }
        this.plugin.getAltarManager().acceptShard(altarLoc, shardType, player.getUniqueId());
        this.plugin.getAltarEffectsManager().onShardAccepted(altarLoc);
        this.msg(player, this.plugin.getConfigManager().getMessage("forge-insert-totem"));
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> this.forgingInProgress.remove(key), 2L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stage 2 → 3: hand-insert vanilla Totem of Undying
    // ─────────────────────────────────────────────────────────────────────────

    private void handleTotemClick(Player player, EquipmentSlot hand, Location altarLoc) {
        if (hand != EquipmentSlot.HAND) {
            return;
        }
        String key = this.altarKey(altarLoc);
        if (this.forgingInProgress.contains(key)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        // Must be a vanilla (non-custom) Totem of Undying
        if (held.getType() != Material.TOTEM_OF_UNDYING || this.plugin.getItemUtil().getTotemType(held) != null) {
            this.msg(player, this.plugin.getConfigManager().getMessage("forge-needs-totem"));
            return;
        }
        String shardType = this.plugin.getAltarManager().getType(altarLoc);
        if (shardType == null) {
            // State inconsistency — reset to active so player can re-insert shard
            this.plugin.getAltarManager().setStage(altarLoc, 1);
            this.plugin.getAltarEffectsManager().onForgingComplete(altarLoc);
            return;
        }
        this.forgingInProgress.add(key);
        // Consume exactly 1 Totem of Undying
        if (held.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
        }
        // Transition to stage 3 — forged totem waits on altar until claimed
        this.plugin.getAltarManager().transitionToTotemReady(altarLoc);
        this.plugin.getAltarEffectsManager().onTotemInserted(altarLoc);
        this.msg(player, this.plugin.getConfigManager().getMessage("forge-totem-ready"));
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> this.forgingInProgress.remove(key), 2L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stage 3 → 1: claim forged totem
    // ─────────────────────────────────────────────────────────────────────────

    private void handleTotemReadyClaim(Player player, EquipmentSlot hand, Location altarLoc) {
        if (hand != EquipmentSlot.HAND) {
            return;
        }
        String key = this.altarKey(altarLoc);
        if (this.forgingInProgress.contains(key)) {
            return;
        }
        UUID owner = this.plugin.getAltarManager().getOwner(altarLoc);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            this.msg(player, this.plugin.getConfigManager().getMessage("forge-wrong-owner"));
            return;
        }
        String shardType = this.plugin.getAltarManager().getType(altarLoc);
        if (shardType == null) {
            // State inconsistency — reset to active
            this.plugin.getAltarManager().setStage(altarLoc, 1);
            this.plugin.getAltarEffectsManager().onForgingComplete(altarLoc);
            return;
        }
        this.forgingInProgress.add(key);
        this.giveItem(player, this.plugin.getItemUtil().createForgedTotem(shardType));
        this.plugin.getAltarManager().completeForging(altarLoc);
        this.plugin.getAltarEffectsManager().onForgingComplete(altarLoc);
        this.msg(player, this.plugin.getConfigManager().getMessage("forge-complete"));
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> this.forgingInProgress.remove(key), 2L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Repair GUI (Wither Skull → ritual start) — unchanged from original
    // ─────────────────────────────────────────────────────────────────────────

    private void openRepairGUI(Player player, Location altarLoc) {
        if (this.repairGUILocs.containsKey(player.getUniqueId())) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, (Component) this.legacy("&5Altar Repair"));
        this.fillPanes(inv);
        inv.setItem(13, null);
        this.repairGUILocs.put(player.getUniqueId(), altarLoc);
        this.repairGUIInvs.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        Inventory top = event.getView().getTopInventory();
        Inventory repairInv = this.repairGUIInvs.get(id);
        if (repairInv == null || top != repairInv) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == repairInv) {
            if (event.getSlot() == 13) {
                Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> {
                    Location loc = this.repairGUILocs.get(player.getUniqueId());
                    if (loc == null || !player.isOnline()) {
                        return;
                    }
                    this.checkRepairSlot(player, repairInv, loc);
                }, 1L);
            } else {
                event.setCancelled(true);
            }
        } else if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        Inventory repairInv = this.repairGUIInvs.remove(id);
        if (repairInv != null) {
            this.repairGUILocs.remove(id);
            this.returnSlotItem(player, repairInv);
        }
    }

    private void checkRepairSlot(Player player, Inventory inv, Location altarLoc) {
        ItemStack inSlot = inv.getItem(13);
        if (inSlot == null || inSlot.getType() != Material.WITHER_SKELETON_SKULL) {
            return;
        }
        inv.setItem(13, null);
        this.repairGUILocs.remove(player.getUniqueId());
        this.repairGUIInvs.remove(player.getUniqueId());
        player.closeInventory();
        this.plugin.getRitualManager().startRitual(altarLoc, player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String altarKey(Location loc) {
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world")
            + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void fillPanes(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); ++i) {
            inv.setItem(i, pane);
        }
    }

    private void returnSlotItem(Player player, Inventory inv) {
        ItemStack item = inv.getItem(13);
        if (item != null && item.getType() != Material.AIR) {
            inv.setItem(13, null);
            this.giveItem(player, item);
        }
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(
            overflow -> player.getWorld().dropItem(player.getLocation(), overflow)
        );
    }

    private void msg(Player player, String legacy) {
        player.sendMessage((Component) LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}
