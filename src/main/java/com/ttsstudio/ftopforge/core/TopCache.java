package com.ttsstudio.ftopforge.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TopCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<FactionSnapshot> sorted = Collections.emptyList();
    private Map<String, FactionSnapshot> byId = Collections.emptyMap();

    // Snapshot of previous top (before last rebuild) for change detection (Discord events).
    // Volatile read-only views — no lock needed for reads.
    private volatile String prevTop1Id = null;
    private volatile List<String> prevTop10Ids = Collections.emptyList();

    public String previousTop1Id() { return prevTop1Id; }
    public List<String> previousTop10Ids() { return prevTop10Ids; }

    public void rebuild(List<FactionSnapshot> snapshots) {
        // Capture pre-rebuild snapshot for change detection (additive, no breaking changes).
        // Use top(10) which takes the read lock — must be called BEFORE acquiring the write lock
        // to avoid nested-lock deadlock.
        List<FactionSnapshot> oldTop = top(10);
        String oldTop1 = oldTop.isEmpty() ? null : oldTop.get(0).factionId();
        List<String> oldTop10 = new ArrayList<>();
        for (FactionSnapshot s : oldTop) oldTop10.add(s.factionId());
        this.prevTop1Id = oldTop1;
        this.prevTop10Ids = Collections.unmodifiableList(oldTop10);

        List<FactionSnapshot> copy = new ArrayList<>(snapshots);
        copy.sort(Comparator.comparingLong(FactionSnapshot::totalValue).reversed());
        Map<String, FactionSnapshot> idx = new HashMap<>();
        for (FactionSnapshot s : copy) idx.put(s.factionId(), s);
        lock.writeLock().lock();
        try {
            this.sorted = Collections.unmodifiableList(copy);
            this.byId = Collections.unmodifiableMap(idx);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<FactionSnapshot> top(int n) {
        lock.readLock().lock();
        try {
            if (n <= 0 || sorted.isEmpty()) return Collections.emptyList();
            return sorted.subList(0, Math.min(n, sorted.size()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int rankOf(String factionId) {
        lock.readLock().lock();
        try {
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).factionId().equals(factionId)) return i + 1;
            }
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    public FactionSnapshot byId(String factionId) {
        lock.readLock().lock();
        try {
            return byId.get(factionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try { return sorted.size(); } finally { lock.readLock().unlock(); }
    }

    public List<FactionSnapshot> topRange(int fromInclusive, int toExclusive) {
        lock.readLock().lock();
        try {
            int size = sorted.size();
            int from = Math.max(0, fromInclusive);
            int to = Math.min(size, toExclusive);
            if (from >= to) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<>(sorted.subList(from, to)));
        } finally {
            lock.readLock().unlock();
        }
    }

    public AvgBreakdown averagesTop10() {
        lock.readLock().lock();
        try {
            int n = Math.min(10, sorted.size());
            if (n == 0) return AvgBreakdown.empty();
            long c = 0, sp = 0, it = 0, bl = 0, ba = 0;
            for (int i = 0; i < n; i++) {
                FactionSnapshot s = sorted.get(i);
                c += s.chunksValue();
                sp += s.spawnersValue();
                it += s.itemsValue();
                bl += s.blocksValue();
                ba += s.balanceValue();
            }
            return new AvgBreakdown(c / n, sp / n, it / n, bl / n, ba / n);
        } finally {
            lock.readLock().unlock();
        }
    }
}
