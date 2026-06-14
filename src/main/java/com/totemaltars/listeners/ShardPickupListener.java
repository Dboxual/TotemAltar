package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ShardPickupListener
implements Listener {
    private final TotemAltars plugin;

    public ShardPickupListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (!this.plugin.getItemUtil().isTotemShard(item)) {
            return;
        }
        if (!this.plugin.getConfigManager().isGuideBookEnabled()) {
            return;
        }
        if (this.plugin.getPlayerDataManager().hasReceivedGuide(player.getUniqueId())) {
            return;
        }
        // Mark immediately to prevent duplicate triggers from multi-item stacks
        this.plugin.getPlayerDataManager().markGuideReceived(player.getUniqueId());
        // Defer 1 tick so the shard is already in the player's inventory before we try to add the book
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.giveGuideBook(player);
            this.playGuideSound(player);
        }, 1L);
    }

    private void giveGuideBook(Player player) {
        ItemStack book = this.plugin.getItemUtil().createGuideBook();
        player.getInventory().addItem(book).values().forEach(
            overflow -> player.getWorld().dropItem(player.getLocation(), overflow)
        );
    }

    private void playGuideSound(Player player) {
        String soundName = this.plugin.getConfigManager().getGuideBookSound();
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }
}
