/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.persistence.PersistentDataType
 */
package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class RitualListener
implements Listener {
    private final TotemAltars plugin;

    public RitualListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getPersistentDataContainer().has(this.plugin.getRitualManager().RITUAL_MOB_KEY, PersistentDataType.BYTE)) {
            return;
        }
        this.plugin.getRitualManager().onRitualMobKilled((Entity)entity);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.getRitualManager().collapseRitualsOwnedBy(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material mat = block.getType();
        if (mat != Material.DAMAGED_ANVIL && mat != Material.ANVIL) {
            return;
        }
        Location loc = block.getLocation();
        if (!this.plugin.getAltarManager().isAltar(loc)) {
            return;
        }
        if (this.plugin.getRitualManager().isRitualActive(loc)) {
            this.plugin.getRitualManager().collapseRitual(loc);
        }
        this.plugin.getAltarEffectsManager().onAltarRemoved(loc);
        event.setDropItems(false);
        block.getWorld().dropItemNaturally(loc, this.plugin.getItemUtil().createAltar());
        this.plugin.getAltarManager().removeAltar(loc);
    }
}

