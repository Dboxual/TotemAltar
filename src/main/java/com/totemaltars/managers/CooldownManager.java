package com.totemaltars.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player, per-totem-type cooldowns using wall-clock time so they
 * survive across config reloads without needing persistence.
 */
public class CooldownManager {

    // Map<PlayerUUID, Map<totemType, expiryEpochMs>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, String totemType) {
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) return false;
        Long expiry = map.get(totemType);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /** Returns seconds remaining on cooldown, or 0 if not on cooldown. */
    public long getRemainingSeconds(UUID playerId, String totemType) {
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) return 0L;
        Long expiry = map.get(totemType);
        if (expiry == null) return 0L;
        long ms = expiry - System.currentTimeMillis();
        return Math.max(0L, (ms + 999) / 1000); // round up to nearest second
    }

    public void setCooldown(UUID playerId, String totemType, long durationSeconds) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                 .put(totemType, System.currentTimeMillis() + durationSeconds * 1000L);
    }

    /** Call on player quit to free the map entry. */
    public void remove(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
