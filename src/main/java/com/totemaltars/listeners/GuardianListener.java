package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GuardianListener implements Listener {

    private final TotemAltars plugin;

    public GuardianListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        // Must be holding a Guardian Totem
        if (!"guardian".equals(plugin.getItemUtil().getTotemType(held))) return;

        // Cancel default interaction (prevent opening trade screens, etc.)
        event.setCancelled(true);

        // Already linked — can't re-link without popping first
        if (plugin.getItemUtil().getGuardianLinkId(held) != null) {
            msg(player, "&cYour Guardian Totem is already linked to someone.");
            return;
        }

        // No self-linking
        if (player.equals(target)) {
            msg(player, plugin.getConfigManager().getMessage("guardian-self-link"));
            return;
        }

        // Both players must be unlinked
        if (hasLinkedGuardianTotem(player)) {
            msg(player, plugin.getConfigManager().getMessage("guardian-already-linked"));
            return;
        }
        if (hasLinkedGuardianTotem(target)) {
            msg(player, plugin.getConfigManager().getMessage("guardian-target-linked"));
            return;
        }

        // Consume one unlinked Guardian Totem from the initiating player
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Both totems share the same linkId — trade-safe
        UUID linkId = UUID.randomUUID();
        giveOrDrop(player, plugin.getItemUtil().createLinkedGuardianTotem(linkId, target.getName()));
        giveOrDrop(target, plugin.getItemUtil().createLinkedGuardianTotem(linkId, player.getName()));

        // Feedback
        String toPlayer = plugin.getConfigManager().getMessage("guardian-link-success")
                .replace("{player}", target.getName());
        String toTarget = plugin.getConfigManager().getMessage("guardian-link-success-target")
                .replace("{player}", player.getName());

        msg(player, toPlayer);
        msg(target, toTarget);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Returns true if the player has at least one linked Guardian Totem in their inventory. */
    private boolean hasLinkedGuardianTotem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!"guardian".equals(plugin.getItemUtil().getTotemType(item))) continue;
            if (plugin.getItemUtil().getGuardianLinkId(item) != null) return true;
        }
        return false;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
    }

    private void msg(Player player, String legacy) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }
}
