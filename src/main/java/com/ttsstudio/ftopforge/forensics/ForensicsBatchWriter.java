package com.ttsstudio.ftopforge.forensics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Buffers {@link ForensicsEvent}s in an unbounded-but-capped queue and flushes
 * them to {@link ForensicsDao} in batches. {@link #enqueue} is safe to call
 * from the main thread (listener); {@link #tryFlush} is intended to run on an
 * async scheduler tick.
 *
 * <p>When the queue exceeds {@code maxQueue}, oldest events are discarded —
 * this trades data fidelity for hard memory bounds, which is the right call
 * for a non-critical observability stream.
 */
public final class ForensicsBatchWriter {

    private final ConcurrentLinkedDeque<ForensicsEvent> queue = new ConcurrentLinkedDeque<>();
    private final ForensicsDao dao;
    private final int batchSize;
    private final int maxQueue;
    private final Logger log;

    public ForensicsBatchWriter(ForensicsDao dao, int batchSize, int maxQueue, Logger log) {
        this.dao = dao;
        this.batchSize = batchSize;
        this.maxQueue = maxQueue;
        this.log = log;
    }

    public void enqueue(ForensicsEvent e) {
        queue.offer(e);
        boolean overflowed = false;
        while (queue.size() > maxQueue) {
            queue.pollFirst();
            overflowed = true;
        }
        if (overflowed) {
            log.warning("forensics queue overflow, oldest events discarded");
        }
    }

    /** Drain up to {@code batchSize} events and write them in a single transaction. */
    public void tryFlush() {
        if (queue.isEmpty()) return;
        List<ForensicsEvent> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            ForensicsEvent e = queue.pollFirst();
            if (e == null) break;
            batch.add(e);
        }
        if (batch.isEmpty()) return;
        try {
            dao.insertBatch(batch);
        } catch (SQLException ex) {
            log.log(Level.WARNING, "forensics flush failed; events lost", ex);
        }
    }

    public int queueSize() { return queue.size(); }
}
