package com.totemaltars.managers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.totemaltars.TotemAltars;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class ShadowArmorManager implements Listener {
    private final TotemAltars plugin;
    private final Set<UUID> activeShadows = new HashSet<>();
    private final Map<UUID, BukkitTask> restoreTasks = new HashMap<>();
    private BukkitTask enforcementTask;

    public ShadowArmorManager(TotemAltars plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Re-send empty armor every second to catch viewers who entered render range while someone is hidden.
        this.enforcementTask = Bukkit.getScheduler().runTaskTimer((Plugin) plugin, this::enforceAllShadows, 20L, 20L);
    }

    public void startShadow(Player player, int durationTicks) {
        UUID id = player.getUniqueId();
        this.cancelTask(id);
        this.activeShadows.add(id);
        sendHiddenEquipment(player);
        BukkitTask task = Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> this.stopShadow(player), (long) durationTicks);
        this.restoreTasks.put(id, task);
    }

    public void stopShadow(Player player) {
        UUID id = player.getUniqueId();
        if (!this.activeShadows.remove(id)) {
            return;
        }
        this.cancelTask(id);
        if (!player.isOnline()) {
            return;
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        sendRealEquipment(player);
    }

    public boolean isInShadow(UUID id) {
        return this.activeShadows.contains(id);
    }

    public void disable() {
        if (this.enforcementTask != null) {
            this.enforcementTask.cancel();
            this.enforcementTask = null;
        }
        for (UUID id : new HashSet<>(this.activeShadows)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                sendRealEquipment(p);
            }
        }
        this.activeShadows.clear();
        this.restoreTasks.values().forEach(BukkitTask::cancel);
        this.restoreTasks.clear();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        this.stopShadow(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.activeShadows.remove(id);
        this.cancelTask(id);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.stopShadow(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        for (UUID hiddenId : new HashSet<>(this.activeShadows)) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden != null && hidden.isOnline() && !hidden.getUniqueId().equals(joiner.getUniqueId())) {
                joiner.sendEquipmentChange(hidden, buildEmptyArmorMap());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!this.activeShadows.contains(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (!this.activeShadows.contains(player.getUniqueId())) {
            return;
        }
        // Re-enforce on the next tick so the client has processed the actual change first.
        Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> sendHiddenEquipment(player));
    }

    private void enforceAllShadows() {
        if (this.activeShadows.isEmpty()) {
            return;
        }
        for (UUID id : new HashSet<>(this.activeShadows)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            sendHiddenEquipment(p);
            if (p.getFireTicks() > 0) {
                p.setFireTicks(0);
            }
        }
    }

    private void sendHiddenEquipment(Player hidden) {
        Map<EquipmentSlot, ItemStack> empty = buildEmptyArmorMap();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(hidden.getUniqueId())) {
                continue;
            }
            viewer.sendEquipmentChange(hidden, empty);
        }
    }

    private void sendRealEquipment(Player player) {
        PlayerInventory inv = player.getInventory();
        Map<EquipmentSlot, ItemStack> real = new EnumMap<>(EquipmentSlot.class);
        real.put(EquipmentSlot.HEAD, orAir(inv.getHelmet()));
        real.put(EquipmentSlot.CHEST, orAir(inv.getChestplate()));
        real.put(EquipmentSlot.LEGS, orAir(inv.getLeggings()));
        real.put(EquipmentSlot.FEET, orAir(inv.getBoots()));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            viewer.sendEquipmentChange(player, real);
        }
    }

    private Map<EquipmentSlot, ItemStack> buildEmptyArmorMap() {
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(EquipmentSlot.class);
        map.put(EquipmentSlot.HEAD, new ItemStack(Material.AIR));
        map.put(EquipmentSlot.CHEST, new ItemStack(Material.AIR));
        map.put(EquipmentSlot.LEGS, new ItemStack(Material.AIR));
        map.put(EquipmentSlot.FEET, new ItemStack(Material.AIR));
        return map;
    }

    private ItemStack orAir(ItemStack item) {
        return (item != null && item.getType() != Material.AIR) ? item.clone() : new ItemStack(Material.AIR);
    }

    private void cancelTask(UUID id) {
        BukkitTask t = this.restoreTasks.remove(id);
        if (t != null) {
            t.cancel();
        }
    }
}
