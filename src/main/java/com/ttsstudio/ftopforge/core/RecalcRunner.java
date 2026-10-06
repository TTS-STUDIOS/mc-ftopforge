package com.ttsstudio.ftopforge.core;

import com.ttsstudio.ftopforge.storage.JdbcRepository;
import com.ttsstudio.ftopforge.storage.RecalcDao;
import com.ttsstudio.ftopforge.storage.SnapshotDao;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public class RecalcRunner {

    /** Callback invocado al final de cada recalc exitoso (en hilo principal). */
    public interface FinishCallback {
        void onFinish(long recalcId, long finishedAt, java.util.List<FactionSnapshot> orderedTop);
    }

    private final JavaPlugin plugin;
    private final CalculationEngine engine;
    private final RecalcDao recalcDao;
    private final SnapshotDao snapshotDao;
    private final TopCache cache;
    private final boolean broadcastStart, broadcastFinish;
    private final String startMsg, finishMsgTemplate;
    private final long recalcDelayMs; // interval between recalcs in ms

    private final java.util.List<FinishCallback> finishCallbacks =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    // State for GUI polling
    private volatile boolean running = false;
    private volatile long lastFinishedMs = 0L;

    public RecalcRunner(JavaPlugin plugin, CalculationEngine engine, JdbcRepository repo, TopCache cache,
                        boolean broadcastStart, boolean broadcastFinish,
                        String startMsg, String finishMsgTemplate) {
        this(plugin, engine, repo, cache, broadcastStart, broadcastFinish, startMsg, finishMsgTemplate, 1800L);
    }

    public RecalcRunner(JavaPlugin plugin, CalculationEngine engine, JdbcRepository repo, TopCache cache,
                        boolean broadcastStart, boolean broadcastFinish,
                        String startMsg, String finishMsgTemplate, long recalcDelaySeconds) {
        this.plugin = plugin;
        this.engine = engine;
        this.recalcDao = new RecalcDao(repo);
        this.snapshotDao = new SnapshotDao(repo);
        this.cache = cache;
        this.broadcastStart = broadcastStart;
        this.broadcastFinish = broadcastFinish;
        this.startMsg = startMsg;
        this.finishMsgTemplate = finishMsgTemplate;
        this.recalcDelayMs = recalcDelaySeconds * 1000L;
    }

    // --- GUI-facing state getters ---

    /** True while a recalc is currently in progress. */
    public boolean isRunning() { return running; }

    /** System.currentTimeMillis() when the last recalc completed, or 0 if never. */
    public long lastFinishedAt() { return lastFinishedMs; }

    /** Estimated time when the next recalc will run.
     *  Returns lastFinishedMs + delay, or 0 if a recalc has never completed. */
    public long nextScheduledAt() {
        if (lastFinishedMs <= 0) return 0L;
        return lastFinishedMs + recalcDelayMs;
    }

    /** Registra un callback que se invocará tras cada recalc exitoso (en hilo principal). */
    public void onFinish(FinishCallback cb) {
        if (cb != null) finishCallbacks.add(cb);
    }

    /** Returns false if a recalc is already running (caller should warn). */
    public boolean trigger() {
        // Frozen while rewards are paid: asking first keeps a row and a broadcast
        // from being written for a recalculation that will not run.
        if (engine.isRunning() || engine.isFrozen()) return false;

        running = true;
        long startedNanos = System.nanoTime();
        long recalcId;
        try {
            recalcId = recalcDao.startNew();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not insert ftf_recalcs row: " + e.getMessage());
            running = false;
            return false;
        }

        if (broadcastStart && startMsg != null) Bukkit.broadcastMessage(startMsg);

        boolean ok = engine.tryStart(snapshots -> {
            // back on main thread (BukkitScheduler.runTaskTimer is sync)
            try {
                for (FactionSnapshot s : snapshots) snapshotDao.upsert(s, recalcId);
            } catch (SQLException e) {
                plugin.getLogger().severe("Snapshot persist failed: " + e.getMessage());
            }
            cache.rebuild(snapshots);
            long ms = (System.nanoTime() - startedNanos) / 1_000_000L;
            try {
                recalcDao.markFinished(recalcId, ms, snapshots.size());
            } catch (SQLException e) {
                plugin.getLogger().warning("markFinished failed: " + e.getMessage());
            }
            if (broadcastFinish && finishMsgTemplate != null) {
                Bukkit.broadcastMessage(finishMsgTemplate.replace("%duration%", String.valueOf(ms / 1000L)));
            }
            plugin.getLogger().info("[recalc] done id=" + recalcId + " factions=" + snapshots.size() + " ms=" + ms);
            lastFinishedMs = System.currentTimeMillis();
            for (FinishCallback cb : finishCallbacks) {
                try { cb.onFinish(recalcId, lastFinishedMs, snapshots); }
                catch (Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "[recalc] onFinish callback failed", t);
                }
            }
            running = false;
        });
        if (!ok) running = false;
        return ok;
    }

    /** Puts the ranking saved by the last finished recalculation back after a restart. */
    public int restoreOnEnable() {
        try {
            java.util.List<FactionSnapshot> saved = snapshotDao.loadLatest();
            if (!saved.isEmpty()) cache.rebuild(saved);
            return saved.size();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not restore the saved ranking: " + e.getMessage());
            return 0;
        }
    }

    public void recoverStaleOnEnable() {
        try {
            int n = recalcDao.markStaleAsAborted();
            if (n > 0) plugin.getLogger().info("Marked " + n + " stale recalc rows as aborted on startup");
        } catch (SQLException e) {
            plugin.getLogger().warning("recoverStaleOnEnable failed: " + e.getMessage());
        }
    }
}
