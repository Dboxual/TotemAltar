package com.totemaltars.expansion;

import com.totemaltars.TotemAltars;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for TotemAltars.
 *
 *   %totemaltars_cooldown%         — seconds remaining on the global cooldown
 *   %totemaltars_cooldown_bar%     — progress bar for the global cooldown
 */
public class TotemAltarsExpansion extends PlaceholderExpansion {

    private static final int BAR_LENGTH = 10;

    private final TotemAltars plugin;

    public TotemAltarsExpansion(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "totemaltars"; }
    @Override public @NotNull String getAuthor()     { return "Admin"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String lower = params.toLowerCase();
        if (lower.equals("cooldown")) {
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), "__global__");
            return String.valueOf(remaining);
        }
        if (lower.equals("cooldown_bar")) {
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), "__global__");
            long max       = plugin.getConfigManager().getGlobalCooldown();
            return buildBar(remaining, max);
        }
        return null;
    }

    private String buildBar(long remaining, long max) {
        if (max <= 0) return "§a" + "█".repeat(BAR_LENGTH);
        double progress = 1.0 - ((double) remaining / max);
        int filled = (int) Math.round(progress * BAR_LENGTH);
        int empty  = BAR_LENGTH - filled;
        return "§a" + "█".repeat(Math.max(0, filled)) + "§7" + "█".repeat(Math.max(0, empty));
    }
}
