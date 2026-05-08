package com.totemaltars.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.totemaltars.TotemAltars;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the visual armor hiding for Shadow Totem via ProtocolLib.
 *
 * Strategy:
 *   1. On shadow start — add player to activeShadows, call setInvisible(true),
 *      and push fake empty-armor packets to every nearby player immediately.
 *   2. Packet adapter — intercepts all outgoing ENTITY_EQUIPMENT packets for
 *      shadowed players and strips armor slots.  This covers any player who
 *      enters render range while the effect is active.
 *   3. On shadow end — remove from activeShadows FIRST (so the adapter passes
 *      through real data), call setInvisible(false) (which triggers the server
 *      to re-send entity state naturally), then push an explicit restore packet
 *      so nearby players see armor again without any gap.
 *   4. Cleaned up on player death, logout, world change, and plugin disable.
 *
 * Armor is NEVER removed from inventory — only its visual representation is
 * hidden via fake packets sent to other players.
 */
public class ShadowArmorManager implements Listener {

    private final TotemAltars plugin;
    private final ProtocolManager protocolManager;

    // Thread-safe because the packet adapter fires on the Netty I/O thread
    // while startShadow/stopShadow run on the main server thread.
    private final Set<UUID> activeShadows = ConcurrentHashMap.newKeySet();

    // Restore tasks — only touched on the main thread
    private final Map<UUID, BukkitTask> restoreTasks = new HashMap<>();

    // Held so we can unregister cleanly on disable
    private final PacketAdapter equipmentAdapter;

    public ShadowArmorManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.equipmentAdapter = buildAdapter();
        protocolManager.addPacketListener(equipmentAdapter);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Begins the shadow effect.  Hides armor visually for durationTicks ticks,
     * then automatically restores.  Armor remains equipped and protective.
     */
    public void startShadow(Player player, int durationTicks) {
        UUID id = player.getUniqueId();
        cancelTask(id); // cancel any lingering task (safety — cooldowns prevent overlap)

        activeShadows.add(id);
        player.setInvisible(true);

        // Push fake empty-armor to players already in range
        sendArmorPacket(player, true);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> stopShadow(player), durationTicks);
        restoreTasks.put(id, task);
    }

    /**
     * Ends the shadow effect and restores armor visibility.
     * Safe to call redundantly — does nothing if already stopped.
     */
    public void stopShadow(Player player) {
        UUID id = player.getUniqueId();
        if (!activeShadows.remove(id)) return;
        cancelTask(id);

        if (!player.isOnline()) return;

        // Remove from active set BEFORE un-hiding so that when Paper re-broadcasts
        // the entity state (including equipment), the adapter does not strip armor.
        player.setInvisible(false);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Explicit resend so nearby players see armor without waiting for the
        // next server-side equipment broadcast.
        sendArmorPacket(player, false);
    }

    /** Called from TotemAltars.onDisable() to guarantee clean shutdown. */
    public void disable() {
        // Restore all active shadows before unregistering
        for (UUID id : new HashSet<>(activeShadows)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.setInvisible(false);
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                sendArmorPacket(p, false);
            }
        }
        activeShadows.clear();
        restoreTasks.values().forEach(BukkitTask::cancel);
        restoreTasks.clear();
        protocolManager.removePacketListener(equipmentAdapter);
    }

    // ── Packet adapter ────────────────────────────────────────────────────────────

    private PacketAdapter buildAdapter() {
        return new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                int entityId = event.getPacket().getIntegers().read(0);

                // Find the shadowed player this packet is about
                Player shadowed = findShadowedByEntityId(entityId);
                if (shadowed == null) return;

                // Never rewrite packets the shadowed player receives about themselves
                if (shadowed.equals(event.getPlayer())) return;

                // Replace armor slots with AIR — modifying in-place is safe because
                // ProtocolLib gives each receiver their own PacketEvent instance.
                List<Pair<EnumWrappers.ItemSlot, ItemStack>> original =
                        event.getPacket().getSlotStackPairLists().read(0);
                if (original == null || original.isEmpty()) return;

                List<Pair<EnumWrappers.ItemSlot, ItemStack>> stripped = new ArrayList<>(original.size());
                for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : original) {
                    if (isArmorSlot(pair.getFirst())) {
                        stripped.add(new Pair<>(pair.getFirst(), new ItemStack(Material.AIR)));
                    } else {
                        stripped.add(pair);
                    }
                }
                event.getPacket().getSlotStackPairLists().write(0, stripped);
            }
        };
    }

    // ── Packet helpers ────────────────────────────────────────────────────────────

    /**
     * Sends a manual ENTITY_EQUIPMENT packet to every player currently in the
     * same world as the target.  When hide=true all four armor slots are AIR;
     * when hide=false the real inventory contents are used.
     */
    private void sendArmorPacket(Player player, boolean hide) {
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = buildSlots(player, hide);

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, player.getEntityId());
        packet.getSlotStackPairLists().write(0, slots);

        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            try {
                protocolManager.sendServerPacket(nearby, packet);
            } catch (Exception e) {
                plugin.getLogger().warning("ShadowArmorManager: failed to send packet to "
                        + nearby.getName() + " — " + e.getMessage());
            }
        }
    }

    private List<Pair<EnumWrappers.ItemSlot, ItemStack>> buildSlots(Player player, boolean hide) {
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>(4);
        ItemStack air = new ItemStack(Material.AIR);

        list.add(new Pair<>(EnumWrappers.ItemSlot.HEAD,
                hide ? air : orAir(player.getInventory().getHelmet())));
        list.add(new Pair<>(EnumWrappers.ItemSlot.CHEST,
                hide ? air : orAir(player.getInventory().getChestplate())));
        list.add(new Pair<>(EnumWrappers.ItemSlot.LEGS,
                hide ? air : orAir(player.getInventory().getLeggings())));
        list.add(new Pair<>(EnumWrappers.ItemSlot.FEET,
                hide ? air : orAir(player.getInventory().getBoots())));

        return list;
    }

    private boolean isArmorSlot(EnumWrappers.ItemSlot slot) {
        return slot == EnumWrappers.ItemSlot.HEAD
            || slot == EnumWrappers.ItemSlot.CHEST
            || slot == EnumWrappers.ItemSlot.LEGS
            || slot == EnumWrappers.ItemSlot.FEET;
    }

    private ItemStack orAir(ItemStack item) {
        return (item == null) ? new ItemStack(Material.AIR) : item;
    }

    /** Scans activeShadows for a player whose entity ID matches. */
    private Player findShadowedByEntityId(int entityId) {
        for (UUID id : activeShadows) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getEntityId() == entityId) return p;
        }
        return null;
    }

    // ── Cleanup listeners ─────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // On death the player entity stays loaded — clean up gracefully
        stopShadow(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        // Player is disconnecting — we can't send packets, just purge state
        if (activeShadows.remove(id)) {
            cancelTask(id);
            // setInvisible state is lost on disconnect; nothing further to restore
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Entity context resets on world change — stop the effect cleanly
        stopShadow(event.getPlayer());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────────

    private void cancelTask(UUID id) {
        BukkitTask t = restoreTasks.remove(id);
        if (t != null) t.cancel();
    }
}
