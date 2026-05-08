package com.totemaltars.commands;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class TotemAltarsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TYPES       = List.of("blast", "shadow", "storm", "swap", "guardian");
    private static final List<String> ADMIN_SUBS  = List.of("createaltar", "removealtar",
                                                             "giveingredient", "givetotem", "reload");
    private static final List<String> ALL_SUBS    = List.of("createaltar", "removealtar",
                                                             "giveingredient", "givetotem", "reload", "altar");

    private final TotemAltars plugin;

    public TotemAltarsCommand(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendUsage(sender); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // altar: available to players with totemaltars.use
        if (sub.equals("altar")) {
            if (!sender.hasPermission("totemaltars.use")) {
                msg(sender, "&cYou don't have permission.");
                return true;
            }
            if (!(sender instanceof Player player)) {
                msg(sender, "&cThis command can only be used in-game.");
                return true;
            }
            handleAltar(player);
            return true;
        }

        // All other subcommands require totemaltars.admin
        if (!sender.hasPermission("totemaltars.admin")) {
            msg(sender, "&cYou don't have permission.");
            return true;
        }

        switch (sub) {
            case "createaltar"   -> handleCreateAltar(sender);
            case "removealtar"   -> handleRemoveAltar(sender);
            case "giveingredient"-> handleGive(sender, args, false);
            case "givetotem"     -> handleGive(sender, args, true);
            case "reload"        -> handleReload(sender);
            default              -> sendUsage(sender);
        }
        return true;
    }

    // ── Admin: createaltar ────────────────────────────────────────────────────────

    private void handleCreateAltar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&cThis command can only be used in-game.");
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            msg(sender, "&cYou must be looking at an anvil within 5 blocks.");
            return;
        }

        Material mat = target.getType();
        if (mat != Material.ANVIL && mat != Material.CHIPPED_ANVIL && mat != Material.DAMAGED_ANVIL) {
            msg(sender, "&cThat block is not an anvil. Must be ANVIL, CHIPPED_ANVIL, or DAMAGED_ANVIL.");
            return;
        }

        boolean created = plugin.getAltarManager().createAltar(target.getLocation());
        if (!created) {
            msg(sender, "&cA Totem Altar already exists at that location.");
            return;
        }

        Location loc = target.getLocation();
        msg(sender, "&aTotem Altar created at &e"
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "&a.");
    }

    // ── Admin: removealtar ────────────────────────────────────────────────────────

    private void handleRemoveAltar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&cThis command can only be used in-game.");
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            msg(sender, "&cYou must be looking at an altar anvil within 5 blocks.");
            return;
        }

        boolean removed = plugin.getAltarManager().removeAltar(target.getLocation());
        if (!removed) {
            msg(sender, "&cNo Totem Altar found at that location.");
            return;
        }

        Location loc = target.getLocation();
        msg(sender, "&aTotem Altar removed from &e"
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "&a.");
    }

    // ── Player: altar (find nearest) ──────────────────────────────────────────────

    private void handleAltar(Player player) {
        if (plugin.getAltarManager().getAltarCount() == 0) {
            msg(player, "&cNo Totem Altars exist yet.");
            return;
        }

        Location nearest = plugin.getAltarManager().getNearestAltar(player.getLocation());
        if (nearest == null) {
            msg(player, "&cNo Totem Altars found.");
            return;
        }

        String worldName = nearest.getWorld().getName();
        int x = nearest.getBlockX();
        int y = nearest.getBlockY();
        int z = nearest.getBlockZ();

        StringBuilder sb = new StringBuilder("&6Nearest Totem Altar: &e")
                .append(worldName)
                .append("&6, X: &e").append(x)
                .append("&6, Y: &e").append(y)
                .append("&6, Z: &e").append(z);

        if (nearest.getWorld().equals(player.getWorld())) {
            int dist = (int) Math.round(player.getLocation().distance(nearest));
            sb.append(" &7(").append(dist).append(" blocks away)");
        }

        msg(player, sb.toString());
    }

    // ── Admin: giveingredient / givetotem ─────────────────────────────────────────

    private void handleGive(CommandSender sender, String[] args, boolean isTotem) {
        String subcmd = isTotem ? "givetotem" : "giveingredient";
        if (args.length < 3) {
            msg(sender, "&cUsage: /totemaltars " + subcmd
                    + " <player> <blast|shadow|storm|swap|guardian> [amount]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(sender, plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            msg(sender, plugin.getConfigManager().getMessage("invalid-type"));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[3]))); }
            catch (NumberFormatException ignored) {}
        }

        ItemStack item = isTotem
                ? plugin.getItemUtil().createTotem(type)
                : plugin.getItemUtil().createIngredient(type);
        item.setAmount(amount);

        // Drop any overflow at the player's feet rather than silently discarding it
        target.getInventory().addItem(item).values()
                .forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));

        String itemLabel = capitalize(type) + (isTotem ? " Totem" : " Ingredient");
        String feedback = plugin.getConfigManager().getMessage("give-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}",   itemLabel)
                .replace("{player}", target.getName());
        msg(sender, feedback);
    }

    // ── Admin: reload ─────────────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        msg(sender, plugin.getConfigManager().getMessage("reload-success"));
    }

    // ── Tab completion ────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (args.length) {
            case 1 -> {
                List<String> opts = sender.hasPermission("totemaltars.admin") ? ALL_SUBS : List.of("altar");
                yield filterStart(opts, args[0]);
            }
            case 2 -> {
                if (!sender.hasPermission("totemaltars.admin")) yield List.of();
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("giveingredient") || sub.equals("givetotem"))
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                yield List.of();
            }
            case 3 -> {
                if (!sender.hasPermission("totemaltars.admin")) yield List.of();
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("giveingredient") || sub.equals("givetotem"))
                    yield filterStart(TYPES, args[2]);
                yield List.of();
            }
            default -> List.of();
        };
    }

    // ── Utility ───────────────────────────────────────────────────────────────────

    private List<String> filterStart(List<String> list, String prefix) {
        return list.stream()
                   .filter(s -> s.startsWith(prefix.toLowerCase(Locale.ROOT)))
                   .toList();
    }

    private void sendUsage(CommandSender sender) {
        msg(sender, "&eTotemAltars commands:");
        if (sender.hasPermission("totemaltars.admin")) {
            msg(sender, "&7  /totemaltars createaltar   &8- Create altar at the anvil you are looking at");
            msg(sender, "&7  /totemaltars removealtar   &8- Remove the altar you are looking at");
            msg(sender, "&7  /totemaltars giveingredient <player> <blast|shadow|storm|swap|guardian> [amount]");
            msg(sender, "&7  /totemaltars givetotem     <player> <blast|shadow|storm|swap|guardian> [amount]");
            msg(sender, "&7  /totemaltars reload");
        }
        if (sender.hasPermission("totemaltars.use")) {
            msg(sender, "&7  /totemaltars altar         &8- Show the nearest Totem Altar");
        }
    }

    private void msg(CommandSender sender, String legacy) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
