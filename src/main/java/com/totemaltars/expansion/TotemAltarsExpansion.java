package com.totemaltars.expansion;

import com.totemaltars.TotemAltars;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PlaceholderAPI expansion for TotemAltars.
 *
 * Supported placeholders:
 *   %totemaltars_cooldown%               — seconds remaining on the global cooldown
 *   %totemaltars_cooldown_bar%           — progress bar for the global cooldown
 *   %totemaltars_cooldown_global%        — alias for cooldown
 *   %totemaltars_cooldown_bar_global%    — alias for cooldown_bar
 *   %totemaltars_cooldown_<type>%        — always reflects the global cooldown (one cooldown system)
 *   %totemaltars_cooldown_bar_<type>%    — always reflects the global cooldown
 */
public class TotemAltarsExpansion extends PlaceholderExpansion {

    private static final int BAR_LENGTH = 10;

    private static final List<String> TOTEM_TYPES = List.of(
            "blast", "shadow", "storm", "swap", "guardian");

    private final TotemAltars plugin;

    public TotemAltarsExpansion(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "totemaltars"; }
    @Override public @NotNull String getAuthor()     { return "Admin"; }
    @Override public @NotNull String getVersion()    { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String lower = params.toLowerCase();

        // %totemaltars_cooldown% and %totemaltars_cooldown_global%
        if (lower.equals("cooldown") || lower.equals("cooldown_global")) {
            long remaining = getRemainingGlobal(player);
            return String.valueOf(remaining);
        }

        // %totemaltars_cooldown_bar% and %totemaltars_cooldown_bar_global%
        if (lower.equals("cooldown_bar") || lower.equals("cooldown_bar_global")) {
            long remaining = getRemainingGlobal(player);
            long max       = plugin.getConfigManager().getGlobalCooldown();
            return buildBar(remaining, max);
        }

        // %totemaltars_cooldown_<type>% — e.g. cooldown_blast, cooldown_storm
        if (lower.startsWith("cooldown_")) {
            String suffix = lower.substring("cooldown_".length());
            if (TOTEM_TYPES.contains(suffix)) {
                return String.valueOf(getRemainingGlobal(player));
            }
        }

        // %totemaltars_cooldown_bar_<type>% — e.g. cooldown_bar_blast
        if (lower.startsWith("cooldown_bar_")) {
            String suffix = lower.substring("cooldown_bar_".length());
            if (TOTEM_TYPES.contains(suffix)) {
                long remaining = getRemainingGlobal(player);
                long max       = plugin.getConfigManager().getGlobalCooldown();
                return buildBar(remaining, max);
            }
        }

        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private long getRemainingGlobal(Player player) {
        return plugin.getCooldownManager()
                .getRemainingSeconds(player.getUniqueId(), "__global__");
    }

    private String buildBar(long remaining, long max) {
        if (max <= 0) return "§a" + "█".repeat(BAR_LENGTH);
        double progress = 1.0 - ((double) remaining / max);
        int filled = (int) Math.round(progress * BAR_LENGTH);
        int empty  = BAR_LENGTH - filled;
        return "§a" + "█".repeat(Math.max(0, filled))
             + "§7" + "█".repeat(Math.max(0, empty));
    }
}
