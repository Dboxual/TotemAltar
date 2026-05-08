package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import com.totemaltars.gui.AltarGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AltarListener implements Listener {

    private final TotemAltars plugin;
    private final Set<UUID> openAltarGuis = new HashSet<>();

    public AltarListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    // ── Open GUI on right-click ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material mat = block.getType();
        if (mat != Material.ANVIL && mat != Material.CHIPPED_ANVIL && mat != Material.DAMAGED_ANVIL) return;

        if (!plugin.getAltarManager().isAltar(block.getLocation())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        openAltarGuis.add(player.getUniqueId());
        plugin.getAltarGUI().openFor(player);
    }

    // ── Inventory click ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openAltarGuis.contains(player.getUniqueId())) return;

        int rawSlot = event.getRawSlot();
        InventoryAction action = event.getAction();
        Inventory inv = event.getInventory();

        // Clicked inside the altar GUI (slots 0-26)
        if (rawSlot >= 0 && rawSlot < 27) {
            if (rawSlot == AltarGUI.OUTPUT_SLOT) {
                event.setCancelled(true);
                handleCraft(player, inv);
                return;
            }
            if (rawSlot == AltarGUI.TOTEM_SLOT || rawSlot == AltarGUI.INGREDIENT_SLOT) {
                // Block double-click collection so it can't pull from glass-pane slots
                if (action == InventoryAction.COLLECT_TO_CURSOR) {
                    event.setCancelled(true);
                    return;
                }
                // Allow the interaction and update output on next tick
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.getAltarGUI().updateOutput(inv));
                return;
            }
            // All other GUI slots are filler — block everything
            event.setCancelled(true);
            return;
        }

        // Clicked in player inventory — block shift-click into GUI and double-click collect
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
        }
    }

    // ── Inventory drag ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openAltarGuis.contains(player.getUniqueId())) return;

        for (int slot : event.getRawSlots()) {
            if (slot < 27 && slot != AltarGUI.TOTEM_SLOT && slot != AltarGUI.INGREDIENT_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getAltarGUI().updateOutput(event.getInventory()));
    }

    // ── Inventory close: return items ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!openAltarGuis.remove(player.getUniqueId())) return;

        Inventory inv = event.getInventory();
        returnItem(player, inv.getItem(AltarGUI.TOTEM_SLOT));
        returnItem(player, inv.getItem(AltarGUI.INGREDIENT_SLOT));
        inv.setItem(AltarGUI.TOTEM_SLOT, null);
        inv.setItem(AltarGUI.INGREDIENT_SLOT, null);
    }

    // ── Crafting logic ────────────────────────────────────────────────────────────

    private void handleCraft(Player player, Inventory inv) {
        ItemStack totemItem      = inv.getItem(AltarGUI.TOTEM_SLOT);
        ItemStack ingredientItem = inv.getItem(AltarGUI.INGREDIENT_SLOT);

        if (!plugin.getItemUtil().isVanillaTotem(totemItem)) return;
        String ingredientType = plugin.getItemUtil().getIngredientType(ingredientItem);
        if (ingredientType == null) return;

        // Consume exactly one of each input
        if (totemItem.getAmount() > 1) {
            totemItem.setAmount(totemItem.getAmount() - 1);
        } else {
            inv.setItem(AltarGUI.TOTEM_SLOT, null);
        }

        if (ingredientItem.getAmount() > 1) {
            ingredientItem.setAmount(ingredientItem.getAmount() - 1);
        } else {
            inv.setItem(AltarGUI.INGREDIENT_SLOT, null);
        }

        // Give result — drop at feet if inventory is full
        ItemStack result = plugin.getItemUtil().createTotem(ingredientType);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        plugin.getAltarGUI().updateOutput(inv);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        player.getInventory().addItem(item).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
    }
}
