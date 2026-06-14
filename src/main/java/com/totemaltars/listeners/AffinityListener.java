package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import com.totemaltars.utils.ItemUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class AffinityListener
implements Listener {
    private final TotemAltars plugin;

    public AffinityListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        String affinityType = this.plugin.getConfigManager().getAffinityTypeForMob(event.getEntityType().name());
        if (affinityType == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        PlayerInventory inv = killer.getInventory();
        int threshold = this.plugin.getConfigManager().getAffinityThreshold();
        ItemStack offhand = inv.getItemInOffHand();
        if (this.plugin.getItemUtil().isTotemShard(offhand) && !this.plugin.getItemUtil().isMorphed(offhand)) {
            this.applyAffinity(offhand, affinityType, threshold, killer);
            inv.setItemInOffHand(offhand);
        }
    }

    private void applyAffinity(ItemStack shard, String affinityType, int threshold, Player player) {
        this.plugin.getItemUtil().addAffinity(shard, affinityType, 1);
        String morphType = null;
        for (String type : ItemUtil.AFFINITY_TYPES) {
            if (this.plugin.getItemUtil().getAffinity(shard, type) < threshold) continue;
            morphType = type;
            break;
        }
        if (morphType != null) {
            this.plugin.getItemUtil().morphShard(shard, morphType);
            this.notifyMorphComplete(player, morphType);
        } else {
            this.plugin.getItemUtil().updateShardLore(shard);
        }
    }

    private void notifyMorphComplete(Player player, String morphType) {
        String typeName = this.plugin.getItemUtil().getShardTypeName(morphType);
        String raw = this.plugin.getConfigManager().getMessage("shard-morphed")
            .replace("{type}", typeName);
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));

        String soundName = this.plugin.getConfigManager().getShardMorphSound();
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.2f);
        } catch (IllegalArgumentException ignored) {}

        String particleName = this.plugin.getConfigManager().getShardMorphParticle();
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 35, 0.4, 0.6, 0.4, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }
}
