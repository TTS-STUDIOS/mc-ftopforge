package com.ttsstudio.ftopforge.forensics;

import com.ttsstudio.ftopforge.hooks.FactionsUUIDHook;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches {@code (world, chunkX, chunkZ) → factionId} lookups for the forensics
 * listener. Backed by an access-order {@link LinkedHashMap} with TTL eviction
 * on read and a hard ceiling enforced by {@code removeEldestEntry}.
 *
 * <p>Why a cache: every BlockPlace/BlockBreak in a claimed chunk would
 * otherwise hit the Factions {@code Board} singleton on the main thread. The
 * cache is keyed by chunk, so a single player mining/placing in one spot is
 * served from memory after the first lookup.
 */
public final class FactionsClaimResolver {

    private static final long TTL_MS = 30_000L;
    private static final int MAX_ENTRIES = 10_000;

    private static final class CacheEntry {
        final String factionId;
        final long fetchedAt;
        CacheEntry(String factionId, long fetchedAt) {
            this.factionId = factionId;
            this.fetchedAt = fetchedAt;
        }
    }

    private final FactionsUUIDHook hook;
    private final LinkedHashMap<String, CacheEntry> cache;

    public FactionsClaimResolver(FactionsUUIDHook hook) {
        this.hook = hook;
        this.cache = new LinkedHashMap<String, CacheEntry>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    public synchronized String factionAt(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        String key = world.getName() + ":" + chunkX + ":" + chunkZ;
        long now = System.currentTimeMillis();
        CacheEntry e = cache.get(key);
        if (e != null && now - e.fetchedAt < TTL_MS) return e.factionId;
        String f = hook.factionIdAt(world, chunkX, chunkZ);
        cache.put(key, new CacheEntry(f, now));
        return f;
    }
}
