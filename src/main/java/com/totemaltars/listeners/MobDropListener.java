package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import org.bukkit.block.Biome;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class MobDropListener implements Listener {

    private final TotemAltars plugin;
    private final Random random = new Random();

    public MobDropListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Only process kills by a player (so grinders / environment don't count)
        if (event.getEntity().getKiller() == null) return;

        switch (event.getEntityType()) {
            case WITHER_SKELETON -> tryDropBlast(event);
            case CREAKING        -> tryDropShadow(event);
            case ENDERMAN        -> tryDropStorm(event);
            case GUARDIAN        -> tryDropGuardian(event);
            case SHULKER         -> tryDropSwap(event);
            default              -> { /* no custom drop */ }
        }
    }

    // ── Blast ────────────────────────────────────────────────────────────────────

    private void tryDropBlast(EntityDeathEvent event) {
        if (roll("blast")) {
            event.getDrops().add(plugin.getItemUtil().createIngredient("blast"));
        }
    }

    // ── Shadow ───────────────────────────────────────────────────────────────────

    // Creaking note: the Creaking (added 1.21.4) is tied to a Creaking Heart.
    // EntityDeathEvent fires when the mob entity is removed.
    private void tryDropShadow(EntityDeathEvent event) {
        if (roll("shadow")) {
            event.getDrops().add(plugin.getItemUtil().createIngredient("shadow"));
        }
    }

    // ── Storm ────────────────────────────────────────────────────────────────────

    // Extra condition: kill must happen inside a Warped Forest biome OR during
    // active thunder/rain.  Both conditions checked together (either qualifies).
    private void tryDropStorm(EntityDeathEvent event) {
        World world = event.getEntity().getWorld();
        boolean inWarpedForest = isInWarpedForest(event);
        boolean badWeather     = world.isThundering() || world.hasStorm();

        if (!inWarpedForest && !badWeather) return;

        if (roll("storm")) {
            event.getDrops().add(plugin.getItemUtil().createIngredient("storm"));
        }
    }

    // ── Guardian ─────────────────────────────────────────────────────────────────

    private void tryDropGuardian(EntityDeathEvent event) {
        if (roll("guardian")) {
            event.getDrops().add(plugin.getItemUtil().createIngredient("guardian"));
        }
    }

    // ── Swap ─────────────────────────────────────────────────────────────────────

    private void tryDropSwap(EntityDeathEvent event) {
        if (roll("swap")) {
            event.getDrops().add(plugin.getItemUtil().createIngredient("swap"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private boolean roll(String type) {
        return random.nextDouble() < plugin.getConfigManager().getDropChance(type);
    }

    /** Uses the 3D biome API (correct for 1.18+ cave biomes). */
    private boolean isInWarpedForest(EntityDeathEvent event) {
        var loc = event.getEntity().getLocation();
        try {
            Biome biome = loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            return biome == Biome.WARPED_FOREST;
        } catch (Exception e) {
            return false;
        }
    }
}
