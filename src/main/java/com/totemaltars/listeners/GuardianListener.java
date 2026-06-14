/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 */
package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GuardianListener
implements Listener {
    private final TotemAltars plugin;

    public GuardianListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Player)) {
            return;
        }
        Player target = (Player)entity;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!"guardian".equals(this.plugin.getItemUtil().getTotemType(held))) {
            return;
        }
        event.setCancelled(true);
        if (this.plugin.getItemUtil().getGuardianLinkId(held) != null) {
            this.sendMessage(player, this.getMessage("guardian-already-linked"));
            return;
        }
        if (player.equals((Object)target)) {
            this.sendMessage(player, this.getMessage("guardian-self-link"));
            return;
        }
        if (this.hasLinkedGuardianTotem(player)) {
            this.sendMessage(player, this.getMessage("guardian-already-linked"));
            return;
        }
        if (this.hasLinkedGuardianTotem(target)) {
            this.sendMessage(player, this.getMessage("guardian-target-linked").replace("{player}", target.getName()));
            return;
        }
        if (held.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
        }
        UUID linkId = UUID.randomUUID();
        this.giveItem(player, this.plugin.getItemUtil().createLinkedGuardianTotem(linkId, target.getName()));
        this.giveItem(target, this.plugin.getItemUtil().createLinkedGuardianTotem(linkId, player.getName()));
        this.sendMessage(player, this.getMessage("guardian-link-success").replace("{player}", target.getName()));
        this.sendMessage(target, this.getMessage("guardian-link-success-target").replace("{player}", player.getName()));
    }

    private boolean hasLinkedGuardianTotem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!"guardian".equals(this.plugin.getItemUtil().getTotemType(item)) || this.plugin.getItemUtil().getGuardianLinkId(item) == null) continue;
            return true;
        }
        return false;
    }

    private void giveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{item});
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }

    private String getMessage(String key) {
        return this.plugin.getConfigManager().getMessage(key);
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(this.legacy(message));
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}

