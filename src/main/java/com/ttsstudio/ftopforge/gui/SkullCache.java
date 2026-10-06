package com.ttsstudio.ftopforge.gui;

import com.ttsstudio.ftopforge.storage.SkullCacheDao;
import com.ttsstudio.ftopforge.storage.SkullCacheRow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

public class SkullCache {

    // Visible for Testable subclass
    interface Clock { long nowMs(); }

    protected final Plugin plugin;
    protected final SkullCacheDao dao;
    protected final long ttlMs;
    protected final int warmupSize;
    protected final Clock clock;
    protected final Semaphore asyncSlots;
    protected final Map<UUID, Cached> memory = new HashMap<UUID, Cached>();
    protected final Set<UUID> inflight = new HashSet<UUID>();

    private static volatile Field profileField; // CraftMetaSkull.profile (lazy-resolved)

    public SkullCache(Plugin plugin, SkullCacheDao dao, long ttlMs, int warmupSize, int asyncConcurrency) {
        this.plugin = plugin;
        this.dao = dao;
        this.ttlMs = ttlMs;
        this.warmupSize = warmupSize;
        this.clock = new Clock() { public long nowMs() { return System.currentTimeMillis(); } };
        this.asyncSlots = new Semaphore(Math.max(1, asyncConcurrency));
    }

    // Test ctor (package-private)
    SkullCache(Plugin plugin, SkullCacheDao dao, long ttlMs, int warmupSize, Clock clock) {
        this.plugin = plugin;
        this.dao = dao;
        this.ttlMs = ttlMs;
        this.warmupSize = warmupSize;
        this.clock = clock;
        this.asyncSlots = new Semaphore(1);
    }

    public synchronized void warmupSync() throws SQLException {
        if (warmupSize <= 0) return;
        List<SkullCacheRow> rows = dao.loadWarmup(warmupSize);
        long now = clock.nowMs();
        for (SkullCacheRow row : rows) {
            if (now - row.fetchedAt < ttlMs) {
                memory.put(row.uuid, new Cached(row.textureValue, row.fetchedAt));
            }
        }
    }

    public synchronized boolean hasFreshInMemory(UUID uuid, long now) {
        Cached c = memory.get(uuid);
        return c != null && (now - c.fetchedAt) < ttlMs;
    }

    public int purgeExpired() throws SQLException {
        long cutoff = clock.nowMs() - ttlMs;
        return dao.deleteExpired(cutoff);
    }

    /** Returns ItemStack PLAYER_HEAD. If fresh in memory, injects texture via reflection.
     *  Otherwise returns vanilla PLAYER_HEAD (Steve) and enqueues async fetch for next time. */
    public ItemStack getOrFetch(UUID uuid, String name) {
        long now = clock.nowMs();
        Cached fresh;
        synchronized (this) {
            fresh = memory.get(uuid);
            if (fresh != null && (now - fresh.fetchedAt) >= ttlMs) fresh = null;
        }
        if (fresh != null) {
            return applyTexture(buildBaseSkull(name), uuid, name, fresh.textureValue);
        }
        enqueueFetch(uuid, name);
        return buildBaseSkull(name);
    }

    private ItemStack buildBaseSkull(String name) {
        // SKULL_ITEM with data value 3 = player head (1.8 API)
        @SuppressWarnings("deprecation")
        ItemStack stack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null && name != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack applyTexture(ItemStack stack, UUID uuid, String name, String textureValue) {
        try {
            SkullMeta meta = (SkullMeta) stack.getItemMeta();
            if (meta == null) return stack;
            Class<?> gpClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = gpClass.getConstructor(UUID.class, String.class).newInstance(uuid, name);
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Object prop = propClass.getConstructor(String.class, String.class).newInstance("textures", textureValue);
            Object props = profile.getClass().getMethod("getProperties").invoke(profile);
            props.getClass().getMethod("put", Object.class, Object.class).invoke(props, "textures", prop);

            if (profileField == null) {
                Field f = meta.getClass().getDeclaredField("profile");
                f.setAccessible(true);
                profileField = f;
            }
            profileField.set(meta, profile);
            stack.setItemMeta(meta);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "SkullCache.applyTexture failed for " + uuid + ": " + t.getClass().getSimpleName() + " " + t.getMessage());
        }
        return stack;
    }

    private void enqueueFetch(final UUID uuid, final String name) {
        synchronized (this) {
            if (inflight.contains(uuid)) return;
            inflight.add(uuid);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                try {
                    asyncSlots.acquire();
                    try {
                        SkullCacheRow row = dao.get(uuid);
                        long now = clock.nowMs();
                        if (row != null && (now - row.fetchedAt) < ttlMs) {
                            synchronized (SkullCache.this) {
                                memory.put(uuid, new Cached(row.textureValue, row.fetchedAt));
                            }
                            return;
                        }
                        String texValue = fetchFromMojang(uuid);
                        if (texValue != null) {
                            dao.put(uuid, texValue, now);
                            synchronized (SkullCache.this) {
                                memory.put(uuid, new Cached(texValue, now));
                            }
                        }
                    } finally {
                        asyncSlots.release();
                    }
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.WARNING,
                            "SkullCache async fetch failed for " + uuid + ": " + t.getMessage());
                } finally {
                    synchronized (SkullCache.this) {
                        inflight.remove(uuid);
                    }
                }
            }
        });
    }

    private String fetchFromMojang(UUID uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" +
                    uuid.toString().replace("-", "") + "?unsigned=false");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200) {
                if (code == 429 || code >= 500) {
                    plugin.getLogger().warning("Mojang sessionserver HTTP " + code + " for " + uuid + " — skipping");
                }
                return null;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            try {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                return extractTexturesValue(sb.toString());
            } finally {
                r.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** Minimal JSON extraction. Mojang response shape:
     *  {"id":"...","name":"...","properties":[{"name":"textures","value":"<base64>","signature":"..."}]} */
    static String extractTexturesValue(String json) {
        int propsIdx = json.indexOf("\"name\":\"textures\"");
        if (propsIdx < 0) return null;
        int valIdx = json.indexOf("\"value\":\"", propsIdx);
        if (valIdx < 0) return null;
        int start = valIdx + "\"value\":\"".length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    public void flush() {
        // No-op: writes happen inline in async tasks. Hook reserved for future batching.
    }

    protected static class Cached {
        final String textureValue;
        final long fetchedAt;
        Cached(String t, long f) { this.textureValue = t; this.fetchedAt = f; }
    }

    /** Test-only subclass exposing synchronous hooks (warmupSync) and no Bukkit deps. */
    public static class Testable extends SkullCache {
        public Testable(SkullCacheDao dao, long ttlMs, int warmupSize, Clock clock) {
            super(null, dao, ttlMs, warmupSize, clock);
        }
    }
}
