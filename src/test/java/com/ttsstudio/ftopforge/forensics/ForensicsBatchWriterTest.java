package com.ttsstudio.ftopforge.forensics;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ForensicsBatchWriterTest {

    /** In-memory ForensicsDao stub that records every batch insert. */
    private static final class RecordingDao extends ForensicsDao {
        final List<ForensicsEvent> inserted = new ArrayList<>();
        RecordingDao() { super((DataSource) null); }
        @Override
        public void insertBatch(List<ForensicsEvent> events) throws SQLException {
            inserted.addAll(events);
        }
    }

    private static ForensicsEvent evt(long ts) {
        return new ForensicsEvent(ts, UUID.randomUUID(), "p", ForensicsEvent.Action.PLACE,
            "BEACON", "world", 0, 64, 0, 0, 0, "facA", false);
    }

    @Test
    void flushBySize_drainsBatchSize() {
        RecordingDao dao = new RecordingDao();
        ForensicsBatchWriter w = new ForensicsBatchWriter(dao, 100, 10_000, Logger.getLogger("test"));
        for (int i = 0; i < 100; i++) w.enqueue(evt(i));
        w.tryFlush();
        assertEquals(100, dao.inserted.size());
        assertEquals(0, w.queueSize());
    }

    @Test
    void tryFlushManual_drainsAvailableUpToBatchSize() {
        RecordingDao dao = new RecordingDao();
        ForensicsBatchWriter w = new ForensicsBatchWriter(dao, 100, 10_000, Logger.getLogger("test"));
        for (int i = 0; i < 50; i++) w.enqueue(evt(i));
        w.tryFlush();
        assertEquals(50, dao.inserted.size());
        assertEquals(0, w.queueSize());
    }

    @Test
    void overflowDiscardsOldest_keepsMostRecent() {
        RecordingDao dao = new RecordingDao();
        ForensicsBatchWriter w = new ForensicsBatchWriter(dao, 100, 5, Logger.getLogger("test"));
        for (int i = 0; i < 10; i++) w.enqueue(evt(i));
        assertEquals(5, w.queueSize());
        w.tryFlush();
        assertEquals(5, dao.inserted.size());
        // oldest 5 (ts 0..4) discarded → remaining ts 5..9
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 5L, dao.inserted.get(i).ts);
        }
    }
}
