package com.ttsstudio.ftopforge.discord;

import com.ttsstudio.ftopforge.meta.MetaDao;

import java.sql.SQLException;

/**
 * Tracks last-fired timestamps for Discord event types via the ftf_meta KV store.
 * Used to suppress duplicate event posts within a configured cooldown window.
 */
public class EventCooldownStore {
    private static final String KEY_PREFIX = "discord.cooldown.";
    private final MetaDao meta;

    public EventCooldownStore(MetaDao meta) { this.meta = meta; }

    /** Returns true if event may fire now; updates last-fired if so. */
    public boolean tryFire(String eventType, int cooldownHours, long nowMs) throws SQLException {
        if (cooldownHours <= 0) {
            meta.putLong(KEY_PREFIX + eventType, nowMs);
            return true;
        }
        java.util.Optional<Long> lastOpt = meta.getLong(KEY_PREFIX + eventType);
        if (lastOpt.isPresent()) {
            long last = lastOpt.get();
            if (nowMs - last < (long) cooldownHours * 3_600_000L) return false;
        }
        meta.putLong(KEY_PREFIX + eventType, nowMs);
        return true;
    }
}
