/*
 * Decompiled with CFR 0.152.
 */
package com.totemaltars.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<UUID, Map<String, Long>>();

    public boolean isOnCooldown(UUID playerId, String key) {
        Map<String, Long> map = this.cooldowns.get(playerId);
        if (map == null) {
            return false;
        }
        Long expiry = map.get(key);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public long getRemainingSeconds(UUID playerId, String key) {
        Map<String, Long> map = this.cooldowns.get(playerId);
        if (map == null) {
            return 0L;
        }
        Long expiry = map.get(key);
        if (expiry == null) {
            return 0L;
        }
        long ms = expiry - System.currentTimeMillis();
        return Math.max(0L, (ms + 999L) / 1000L);
    }

    public void setCooldown(UUID playerId, String key, long durationSeconds) {
        this.cooldowns.computeIfAbsent(playerId, k -> new HashMap()).put(key, System.currentTimeMillis() + durationSeconds * 1000L);
    }

    public void remove(UUID playerId) {
        this.cooldowns.remove(playerId);
    }
}

