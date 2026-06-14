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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles the Bedrock Relic progression path:
 *   1. Activating an inactive relic on a Bedrock block.
 *   2. Binding an activated relic to a repaired Totem Altar.
 *   3. The BedRocker enchanting GUI after binding completes.
 *
 * Runs at NORMAL priority so it fires before AltarInteractionListener (HIGH).
 * When this listener fully handles an altar click it cancels the event,
 * preventing the altar listener from firing (ignoreCancelled = true).
 */
public class BedrockRelicListener implements Listener {
    private final TotemAltars plugin;

    // Active binding sequences: playerUUID → BindingState
    private final Map<UUID, BindingState> activeBindings = new HashMap<>();
    // Altars currently being bound (prevents two players binding the same altar)
    private final Set<String> bindingAltars = new HashSet<>();

    // Enchanting GUI state: playerUUID → altar location / inventory
    private final Map<UUID, Location> enchantGUILocs = new HashMap<>();
    private final Map<UUID, Inventory> enchantGUIInvs = new HashMap<>();

    // Slots in the 27-slot enchanting GUI
    private static final int SLOT_RELIC   = 11;
    private static final int SLOT_PICKAXE = 15;
    private static final int SLOT_CONFIRM = 13;

    public BedrockRelicListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main interaction dispatch (NORMAL priority — runs before AltarInteractionListener)
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        Material blockType = block.getType();

        // ── Case 1: Activate an inactive relic on a Bedrock block ──────────
        if (blockType == Material.BEDROCK
                && plugin.getItemUtil().isBedrockRelic(held)
                && !plugin.getItemUtil().isActivatedBedrockRelic(held)) {
            event.setCancelled(true);
            activateRelic(player, held);
            return;
        }

        // ── Altar cases — only for ANVIL / DAMAGED_ANVIL that are tracked ──
        if (blockType != Material.ANVIL && blockType != Material.DAMAGED_ANVIL) return;
        Location loc = block.getLocation();
        if (!plugin.getAltarManager().isAltar(loc)) return;

        // ── Case 2: Activated relic on altar → binding ──────────────────────
        if (plugin.getItemUtil().isActivatedBedrockRelic(held)) {
            event.setCancelled(true);
            if (!plugin.getAltarManager().isActive(loc)) {
                msg(player, plugin.getConfigManager().getMessage("bedrock-altar-not-active"));
                return;
            }
            if (plugin.getAltarManager().isAwaitingTotem(loc) || plugin.getAltarManager().isTotemReady(loc)) {
                msg(player, plugin.getConfigManager().getMessage("bedrock-altar-busy"));
                return;
            }
            if (bindingAltars.contains(altarKey(loc))) {
                msg(player, "&cAnother binding is already in progress at this altar.");
                return;
            }
            startBinding(player, loc);
            return;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 1: Relic activation on Bedrock
    // ─────────────────────────────────────────────────────────────────────────

    private void activateRelic(Player player, ItemStack relic) {
        plugin.getItemUtil().activateBedrockRelic(relic);
        msg(player, plugin.getConfigManager().getMessage("bedrock-relic-activate"));
        msg(player, plugin.getConfigManager().getMessage("bedrock-relic-activate-2"));

        Location loc = player.getLocation().add(0, 1, 0);
        try {
            Particle p = Particle.valueOf("LAVA");
            player.getWorld().spawnParticle(p, loc, 15, 0.3, 0.3, 0.3, 0.05);
        } catch (Exception ignored) {}
        try {
            Sound sound = Sound.valueOf("BLOCK_STONE_BREAK");
            player.getWorld().playSound(loc, sound, 1.0f, 0.4f);
        } catch (Exception ignored) {}
        try {
            Sound sound2 = Sound.valueOf("ENTITY_PLAYER_LEVELUP");
            player.playSound(player.getLocation(), sound2, 0.8f, 1.8f);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 2: Altar binding sequence
    // ─────────────────────────────────────────────────────────────────────────

    private void startBinding(Player player, Location altarLoc) {
        int bindingTicks = plugin.getConfigManager().getBedrockBindingTicks();
        int totalSeconds = Math.max(1, bindingTicks / 20);
        String altKey = altarKey(altarLoc);

        BossBar bar = Bukkit.createBossBar(
            "Binding Bedrock Relic — " + totalSeconds + "s remaining",
            BarColor.YELLOW, BarStyle.SEGMENTED_10, new BarFlag[0]
        );
        bar.setProgress(0.0);
        bar.addPlayer(player);
        bindingAltars.add(altKey);

        UUID playerUuid = player.getUniqueId();
        BindingState state = new BindingState(altarLoc, altKey, bar);
        activeBindings.put(playerUuid, state);

        int[] remaining = { totalSeconds };

        state.task = Bukkit.getScheduler().runTaskTimer((Plugin) plugin, () -> {
            remaining[0]--;
            Player p = Bukkit.getPlayer(playerUuid);
            if (p == null || !p.isOnline()) {
                cleanupBinding(playerUuid, state);
                return;
            }

            double progress = 1.0 - ((double) remaining[0] / totalSeconds);
            bar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
            bar.setTitle("Binding Bedrock Relic — " + remaining[0] + "s remaining");

            Location center = altarLoc.clone().add(0.5, 0.5, 0.5);
            try {
                Particle particle = Particle.valueOf("FLAME");
                altarLoc.getWorld().spawnParticle(particle, center, 6, 0.35, 0.35, 0.35, 0.02);
            } catch (Exception ignored) {}
            try {
                Particle particle2 = Particle.valueOf("PORTAL");
                altarLoc.getWorld().spawnParticle(particle2, center, 4, 0.2, 0.4, 0.2, 0.1);
            } catch (Exception ignored) {}

            if (remaining[0] <= 0) {
                cleanupBinding(playerUuid, state);
                msg(p, plugin.getConfigManager().getMessage("bedrock-binding-complete"));
                altarLoc.getWorld().strikeLightningEffect(center);
                try {
                    Sound sound = Sound.valueOf("BLOCK_BEACON_ACTIVATE");
                    altarLoc.getWorld().playSound(center, sound, 1.5f, 0.6f);
                } catch (Exception ignored) {}
                try {
                    Particle particle = Particle.valueOf("TOTEM_OF_UNDYING");
                    altarLoc.getWorld().spawnParticle(particle, center, 60, 0.5, 1.0, 0.5, 0.3);
                } catch (Exception ignored) {}
                openEnchantingGUI(p, altarLoc);
            }
        }, 20L, 20L);
    }

    private void cleanupBinding(UUID playerUuid, BindingState state) {
        activeBindings.remove(playerUuid);
        bindingAltars.remove(state.altarKey);
        state.bossBar.removeAll();
        if (state.task != null) state.task.cancel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 3: BedRocker enchanting GUI
    // ─────────────────────────────────────────────────────────────────────────

    private void openEnchantingGUI(Player player, Location altarLoc) {
        if (enchantGUILocs.containsKey(player.getUniqueId())) return;

        Inventory inv = Bukkit.createInventory(null, 27, legacy("&9BedRocker Enchanting"));
        fillPanes(inv);

        // Clear the two input slots and the confirm button
        inv.setItem(SLOT_RELIC, createSlotLabel(Material.GRAY_STAINED_GLASS_PANE, "&7[Activated Bedrock Relic]", "&8Place here"));
        inv.setItem(SLOT_PICKAXE, createSlotLabel(Material.GRAY_STAINED_GLASS_PANE, "&7[Netherite Pickaxe]", "&8Place here"));
        inv.setItem(SLOT_CONFIRM, createConfirmButton());

        // Replace placeholder panes with empty slots so players can place items
        inv.setItem(SLOT_RELIC, null);
        inv.setItem(SLOT_PICKAXE, null);

        enchantGUILocs.put(player.getUniqueId(), altarLoc);
        enchantGUIInvs.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity he = event.getWhoClicked();
        if (!(he instanceof Player player)) return;

        UUID id = player.getUniqueId();
        Inventory top = event.getView().getTopInventory();
        Inventory enchantInv = enchantGUIInvs.get(id);
        if (enchantInv == null || top != enchantInv) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == enchantInv) {
            int slot = event.getSlot();
            if (slot == SLOT_RELIC || slot == SLOT_PICKAXE) {
                // Allow interaction — player may freely put/remove items
            } else if (slot == SLOT_CONFIRM) {
                event.setCancelled(true);
                // Schedule 1-tick delay to let any pending cursor-swap settle
                Bukkit.getScheduler().runTaskLater((Plugin) plugin, () -> {
                    Location loc = enchantGUILocs.get(id);
                    if (loc == null || !player.isOnline()) return;
                    checkEnchant(player, enchantInv, loc);
                }, 1L);
            } else {
                event.setCancelled(true);
            }
        } else if (event.isShiftClick()) {
            // Prevent shift-clicking items from player inventory into wrong slots
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity he = event.getPlayer();
        if (!(he instanceof Player player)) return;

        UUID id = player.getUniqueId();
        Inventory enchantInv = enchantGUIInvs.remove(id);
        if (enchantInv == null) return;

        enchantGUILocs.remove(id);
        // Return any items left in the two input slots
        returnItem(player, enchantInv, SLOT_RELIC);
        returnItem(player, enchantInv, SLOT_PICKAXE);
    }

    private void checkEnchant(Player player, Inventory inv, Location altarLoc) {
        ItemStack relicSlot   = inv.getItem(SLOT_RELIC);
        ItemStack pickaxeSlot = inv.getItem(SLOT_PICKAXE);

        if (relicSlot == null || !plugin.getItemUtil().isActivatedBedrockRelic(relicSlot)) {
            msg(player, plugin.getConfigManager().getMessage("bedrock-enchant-needs-relic"));
            return;
        }
        if (pickaxeSlot == null || pickaxeSlot.getType() != Material.NETHERITE_PICKAXE) {
            msg(player, plugin.getConfigManager().getMessage("bedrock-enchant-needs-pickaxe"));
            return;
        }
        if (plugin.getItemUtil().hasBedRocker(pickaxeSlot)) {
            msg(player, plugin.getConfigManager().getMessage("bedrock-enchant-pickaxe-already"));
            return;
        }

        // Consume exactly 1 relic
        if (relicSlot.getAmount() == 1) {
            inv.setItem(SLOT_RELIC, null);
        } else {
            relicSlot.setAmount(relicSlot.getAmount() - 1);
        }

        // Apply BedRocker to the pickaxe in-slot
        plugin.getItemUtil().applyBedRocker(pickaxeSlot);

        // Close GUI and give back the pickaxe (it was modified in the slot)
        enchantGUILocs.remove(player.getUniqueId());
        enchantGUIInvs.remove(player.getUniqueId());
        player.closeInventory();

        giveItem(player, pickaxeSlot);
        inv.setItem(SLOT_PICKAXE, null);

        msg(player, plugin.getConfigManager().getMessage("bedrock-enchant-success"));

        Location center = altarLoc.clone().add(0.5, 0.5, 0.5);
        try {
            altarLoc.getWorld().strikeLightningEffect(center);
        } catch (Exception ignored) {}
        try {
            Sound sound = Sound.valueOf("ENTITY_PLAYER_LEVELUP");
            altarLoc.getWorld().playSound(center, sound, 1.2f, 0.7f);
        } catch (Exception ignored) {}
        try {
            Particle p = Particle.valueOf("ENCHANT");
            altarLoc.getWorld().spawnParticle(p, center, 50, 0.5, 0.8, 0.5, 0.2);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player quit — clean up any open sessions
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        BindingState bs = activeBindings.get(id);
        if (bs != null) cleanupBinding(id, bs);
        enchantGUILocs.remove(id);
        enchantGUIInvs.remove(id);
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
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy("&6✦ Enchant Pickaxe ✦").decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
            legacy("&7Slot 1: Activated Bedrock Relic").decoration(TextDecoration.ITALIC, false),
            legacy("&7Slot 2: Netherite Pickaxe").decoration(TextDecoration.ITALIC, false),
            legacy("&8Place items then click to enchant.").decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("unused")
    private ItemStack createSlotLabel(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(legacy(lore).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private void returnItem(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            inv.setItem(slot, null);
            giveItem(player, item);
        }
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(
            overflow -> player.getWorld().dropItem(player.getLocation(), overflow)
        );
    }

    private void msg(Player player, String raw) {
        player.sendMessage((Component) LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────────────────────────────────────

    private static class BindingState {
        final Location altarLoc;
        final String altarKey;
        final BossBar bossBar;
        BukkitTask task;

        BindingState(Location altarLoc, String altarKey, BossBar bossBar) {
            this.altarLoc = altarLoc;
            this.altarKey = altarKey;
            this.bossBar = bossBar;
        }
    }
}
