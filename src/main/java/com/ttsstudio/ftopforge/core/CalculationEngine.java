package com.ttsstudio.ftopforge.core;

import com.ttsstudio.ftopforge.hooks.FactionsUUIDHook;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs ONE recalc end-to-end as a chain of throttled async ticks.
 * Per tick: pop N chunk-jobs from the queue, schedule sync loads (chunk reads must be on main thread for safety),
 * scan tile entities, accumulate into the per-faction Builder. After all chunks done, do balances tick, then finalize.
 * No Thread.suspend, no sleep, no thread.
 */
public class CalculationEngine {

    private final JavaPlugin plugin;
    private final FactionsUUIDHook factions;
    private final ChunkValuator chunkValuator;
    private final BalanceAggregator balances;
    private final int chunksPerSec;
    private final int balancesPerSec;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    public CalculationEngine(JavaPlugin plugin, FactionsUUIDHook factions, ChunkValuator chunkValuator,
                             BalanceAggregator balances, int chunksPerSec, int balancesPerSec) {
        this.plugin = plugin;
        this.factions = factions;
        this.chunkValuator = chunkValuator;
        this.balances = balances;
        this.chunksPerSec = Math.max(1, chunksPerSec);
        this.balancesPerSec = Math.max(1, balancesPerSec);
    }

    public boolean isRunning() { return running.get(); }

    /** When frozen, tryStart returns false (used during weekly payout to avoid stale snapshots mid-write). */
    public void setFrozen(boolean f) { frozen.set(f); }
    public boolean isFrozen() { return frozen.get(); }

    /** Kick off one recalc. onDone is called on the main thread with the list of snapshots. */
    public boolean tryStart(Consumer<List<FactionSnapshot>> onDone) {
        if (frozen.get()) return false;
        if (!running.compareAndSet(false, true)) return false;

        // 1. Build the work queue on the main thread (Factions API isn't thread-safe).
        Deque<ChunkJob> chunkQueue = new ArrayDeque<>();
        Map<String, FactionSnapshot.Builder> builders = new HashMap<>();
        Deque<BalanceJob> balanceQueue = new ArrayDeque<>();

        for (Faction f : factions.allRealFactions()) {
            FactionSnapshot.Builder b = FactionSnapshot.builder(f.getId(), f.getTag());
            UUID lu = factions.leaderUuid(f);
            b.leader(lu == null ? null : lu.toString(), factions.leaderName(f));
            builders.put(f.getId(), b);

            for (FLocation fl : factions.claimsOf(f)) {
                chunkQueue.add(new ChunkJob(f.getId(), fl.getWorldName(), (int) fl.getX(), (int) fl.getZ()));
                b.incChunkCount();
            }
            for (UUID member : factions.memberUuids(f)) {
                balanceQueue.add(new BalanceJob(f.getId(), member));
            }
        }

        plugin.getLogger().info("[recalc] queued " + chunkQueue.size() + " chunks, " + balanceQueue.size() + " balances across " + builders.size() + " factions");

        // 2. Tick chunk jobs at chunksPerSec. Ticks run every second (period 20L).
        BukkitTask[] holder = new BukkitTask[2];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int budget = chunksPerSec;
            while (budget-- > 0 && !chunkQueue.isEmpty()) {
                ChunkJob job = chunkQueue.poll();
                World w = Bukkit.getWorld(job.world);
                if (w == null) continue;
                Chunk ch = w.getChunkAt(job.cx, job.cz);
                boolean wasLoaded = ch.isLoaded();
                if (!wasLoaded) ch.load(false);
                try {
                    chunkValuator.scan(ch, builders.get(job.factionId));
                } finally {
                    if (!wasLoaded) ch.unload(false, false);
                }
            }
            if (chunkQueue.isEmpty()) {
                holder[0].cancel();
                // start balance ticks (same scheduler, new period)
                holder[1] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    int bb = balancesPerSec;
                    while (bb-- > 0 && !balanceQueue.isEmpty()) {
                        BalanceJob bj = balanceQueue.poll();
                        balances.aggregate(bj.member, builders.get(bj.factionId));
                    }
                    if (balanceQueue.isEmpty()) {
                        holder[1].cancel();
                        // finalize
                        java.util.List<FactionSnapshot> out = new java.util.ArrayList<>(builders.size());
                        for (FactionSnapshot.Builder b : builders.values()) out.add(b.build());
                        running.set(false);
                        onDone.accept(out);
                    }
                }, 20L, 20L);
            }
        }, 20L, 20L);

        return true;
    }

    private static final class ChunkJob {
        final String factionId, world; final int cx, cz;
        ChunkJob(String f, String w, int cx, int cz) { this.factionId = f; this.world = w; this.cx = cx; this.cz = cz; }
    }
    private static final class BalanceJob {
        final String factionId; final UUID member;
        BalanceJob(String f, UUID m) { this.factionId = f; this.member = m; }
    }
}
