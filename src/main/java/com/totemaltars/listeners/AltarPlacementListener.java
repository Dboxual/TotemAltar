/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class AltarPlacementListener
implements Listener {
    private final TotemAltars plugin;

    public AltarPlacementListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!this.plugin.getItemUtil().isTotemAltar(item)) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        this.plugin.getAltarManager().registerAltar(loc);
        if (this.plugin.getItemUtil().isPreActivatedAltar(item)) {
            this.plugin.getAltarManager().setActive(loc);
        }
        this.plugin.getAltarEffectsManager().onAltarPlaced(loc, event.getPlayer());
        this.plugin.getLogger().info("Totem Altar placed at " + String.valueOf(loc.toVector()) + " by " + event.getPlayer().getName());
    }
}

