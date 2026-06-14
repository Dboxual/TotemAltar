/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package com.totemaltars.commands;

import com.totemaltars.TotemAltars;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TotemAltarsCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> ADMIN_SUBS = List.of("giveshard", "givealtar", "givefinishedaltar", "givebedrockrelic", "givetotem", "purgelegacy", "reload");
    private static final List<String> PURGE_LEGACY_TARGETS = List.of("all", "loaded");
    private static final List<String> TOTEM_TYPES = List.of("blast", "shadow", "storm", "swap", "guardian", "all");
    private final TotemAltars plugin;

    public TotemAltarsCommand(TotemAltars plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("totemaltars.admin")) {
            this.msg(sender, "&cYou don't have permission.");
            return true;
        }
        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "giveshard": {
                this.handleGiveShard(sender, args);
                break;
            }
            case "givealtar": {
                this.handleGiveAltar(sender, args);
                break;
            }
            case "givefinishedaltar": {
                this.handleGiveFinishedAltar(sender, args);
                break;
            }
            case "givebedrockrelic": {
                this.handleGiveBedrockRelic(sender, args);
                break;
            }
            case "givetotem": {
                this.handleGiveTotem(sender, args);
                break;
            }
            case "purgelegacy": {
                this.handlePurgeLegacy(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            default: {
                this.sendUsage(sender);
            }
        }
        return true;
    }

    private void handleGiveShard(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.msg(sender, "&cUsage: /totemaltars giveshard <player> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        ItemStack shard = this.plugin.getItemUtil().createShard();
        shard.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{shard}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        String feedback = this.plugin.getConfigManager().getMessage("give-success").replace("{amount}", String.valueOf(amount)).replace("{player}", target.getName());
        this.msg(sender, feedback);
    }

    private void handleGiveAltar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.msg(sender, "&cUsage: /totemaltars givealtar <player> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        ItemStack altar = this.plugin.getItemUtil().createAltar();
        altar.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{altar}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        String feedback = this.plugin.getConfigManager().getMessage("give-altar-success").replace("{amount}", String.valueOf(amount)).replace("{player}", target.getName());
        this.msg(sender, feedback);
    }

    private void handleGiveFinishedAltar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.msg(sender, "&cUsage: /totemaltars givefinishedaltar <player> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        ItemStack altar = this.plugin.getItemUtil().createFinishedAltar();
        altar.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{altar}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        String feedback = this.plugin.getConfigManager().getMessage("give-finished-altar-success").replace("{amount}", String.valueOf(amount)).replace("{player}", target.getName());
        this.msg(sender, feedback);
    }

    private void handleGiveBedrockRelic(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.msg(sender, "&cUsage: /totemaltars givebedrockrelic <player>");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        ItemStack relic = this.plugin.getItemUtil().createBedrockRelic();
        target.getInventory().addItem(new ItemStack[]{relic}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        String feedback = this.plugin.getConfigManager().getMessage("give-bedrock-relic-success").replace("{player}", target.getName());
        this.msg(sender, feedback);
    }

    private void handleGiveTotem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            this.msg(sender, "&cUsage: /totemaltars givetotem <player> <blast|shadow|storm|swap|guardian|all> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        String typeArg = args[2].toLowerCase(Locale.ROOT);
        if (typeArg.equals("all")) {
            for (String type : com.totemaltars.utils.ItemUtil.AFFINITY_TYPES) {
                ItemStack totem = this.plugin.getItemUtil().createForgedTotem(type);
                target.getInventory().addItem(new ItemStack[]{totem}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
            }
            this.msg(sender, "&aGiven 1 of each totem type to &e" + target.getName() + "&a.");
            return;
        }
        if (!com.totemaltars.utils.ItemUtil.AFFINITY_TYPES.contains(typeArg)) {
            this.msg(sender, "&cUnknown totem type. Use: blast, shadow, storm, swap, guardian, or all.");
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        ItemStack totem = this.plugin.getItemUtil().createForgedTotem(typeArg);
        totem.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{totem}).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        String displayType = Character.toUpperCase(typeArg.charAt(0)) + typeArg.substring(1);
        this.msg(sender, "&aGiven &e" + amount + "x &f" + displayType + " Totem &ato &e" + target.getName() + "&a.");
    }

    private void handlePurgeLegacy(CommandSender sender, String[] args) {
        if (args.length < 2) {
            this.msg(sender, "&cUsage: /totemaltars purgelegacy <player|all|loaded>");
            return;
        }
        String targetArg = args[1].toLowerCase(Locale.ROOT);
        int removed;
        switch (targetArg) {
            case "all": {
                removed = this.plugin.getLegacyIngredientCleanup().scanOnlinePlayers("command:" + sender.getName() + ":all");
                removed += this.plugin.getLegacyIngredientCleanup().scanLoadedTileInventories("command:" + sender.getName() + ":all");
                this.msg(sender, "&aPurged &e" + removed + " &alegacy ingredient item(s) from online players and loaded containers.");
                break;
            }
            case "loaded": {
                removed = this.plugin.getLegacyIngredientCleanup().scanLoadedTileInventories("command:" + sender.getName() + ":loaded");
                this.msg(sender, "&aPurged &e" + removed + " &alegacy ingredient item(s) from loaded containers.");
                break;
            }
            default: {
                Player target = Bukkit.getPlayer((String)args[1]);
                if (target == null) {
                    this.msg(sender, this.plugin.getConfigManager().getMessage("player-not-found"));
                    return;
                }
                removed = this.plugin.getLegacyIngredientCleanup().scanPlayer(target, "command:" + sender.getName() + ":player:" + target.getName());
                this.msg(sender, "&aPurged &e" + removed + " &alegacy ingredient item(s) from &e" + target.getName() + "&a.");
                break;
            }
        }
    }

    private void handleReload(CommandSender sender) {
        this.plugin.getConfigManager().reload();
        this.msg(sender, this.plugin.getConfigManager().getMessage("reload-success"));
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("totemaltars.admin")) {
            return List.of();
        }
        return switch (args.length) {
            case 1 -> this.filterStart(ADMIN_SUBS, args[0]);
            case 2 -> {
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("giveshard") || sub.equals("givealtar") || sub.equals("givefinishedaltar") || sub.equals("givebedrockrelic") || sub.equals("givetotem")) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                }
                if (sub.equals("purgelegacy")) {
                    List<String> options = new java.util.ArrayList<>(PURGE_LEGACY_TARGETS);
                    options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    yield options.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
                }
                yield List.of();
            }
            case 3 -> {
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("givetotem")) {
                    yield this.filterStart(TOTEM_TYPES, args[2]);
                }
                yield List.of();
            }
            case 4 -> {
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("givetotem") && !args[2].equalsIgnoreCase("all")) {
                    yield List.of("1", "4", "8", "16", "32", "64").stream().filter(n -> n.startsWith(args[3])).toList();
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    private List<String> filterStart(List<String> list, String prefix) {
        return list.stream().filter(s -> s.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }

    private void sendUsage(CommandSender sender) {
        this.msg(sender, "&eTotemAltars commands:");
        this.msg(sender, "&7  /totemaltars giveshard <player> [amount]");
        this.msg(sender, "&7  /totemaltars givealtar <player> [amount]");
        this.msg(sender, "&7  /totemaltars givefinishedaltar <player> [amount]");
        this.msg(sender, "&7  /totemaltars givebedrockrelic <player>");
        this.msg(sender, "&7  /totemaltars givetotem <player> <blast|shadow|storm|swap|guardian|all> [amount]");
        this.msg(sender, "&7  /totemaltars purgelegacy <player|all|loaded>");
        this.msg(sender, "&7  /totemaltars reload");
    }

    private void msg(CommandSender sender, String legacy) {
        sender.sendMessage((Component)LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }
}
