/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Monster
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDeathEvent
 */
package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.Random;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDropListener
implements Listener {
    private final TotemAltars plugin;
    private final Random random = new Random();

    public MobDropListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        double chance = this.plugin.getConfigManager().getShardDropChance();
        if (this.random.nextDouble() < chance) {
            event.getDrops().add(this.plugin.getItemUtil().createShard());
        }
    }
}

