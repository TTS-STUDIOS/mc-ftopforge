package com.ttsstudio.ftopforge.forensics;

import com.ttsstudio.ftopforge.storage.Dialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForensicsDaoTest {
    private HikariDataSource ds;
    private ForensicsDao dao;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(Dialect.SQLITE.forensicsDdl());
            for (String idx : Dialect.SQLITE.forensicsIndexesDdl()) s.execute(idx);
        }
        dao = new ForensicsDao(ds);
    }

    @AfterEach
    void tearDown() { ds.close(); }

    private static ForensicsEvent evt(long ts, String faction, ForensicsEvent.Action action, boolean member) {
        return new ForensicsEvent(ts, UUID.randomUUID(), "player" + ts, action,
            "BEACON", "world", 1, 64, 2, 0, 0, faction, member);
    }

    @Test
    void queryByFaction_paginatedReturnsCorrectPage() throws Exception {
        List<ForensicsEvent> events = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            events.add(evt(1000L + i, "facA", ForensicsEvent.Action.PLACE, false));
        }
        dao.insertBatch(events);

        List<ForensicsEvent> page0 = dao.queryByFaction("facA", null, false, 0, 10);
        assertEquals(10, page0.size());

        List<ForensicsEvent> page1 = dao.queryByFaction("facA", null, false, 1, 10);
        assertEquals(10, page1.size());

        List<ForensicsEvent> page2 = dao.queryByFaction("facA", null, false, 2, 10);
        assertEquals(5, page2.size());

        // page 0 should contain the most recent (highest ts) since we order by ts DESC
        assertTrue(page0.get(0).ts > page1.get(0).ts);
    }

    @Test
    void deleteOlderThan_removesOldEventsOnly() throws Exception {
        long now = 100L * 86_400_000L;
        long old = now - 60L * 86_400_000L;

        List<ForensicsEvent> oldBatch = new ArrayList<>();
        oldBatch.add(evt(old, "facA", ForensicsEvent.Action.PLACE, false));
        oldBatch.add(evt(old + 1, "facA", ForensicsEvent.Action.BREAK, true));
        dao.insertBatch(oldBatch);

        List<ForensicsEvent> recentBatch = new ArrayList<>();
        recentBatch.add(evt(now, "facA", ForensicsEvent.Action.PLACE, false));
        dao.insertBatch(recentBatch);

        long cutoff = now - 30L * 86_400_000L;
        int deleted = dao.deleteOlderThan(cutoff);
        assertEquals(2, deleted);

        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ftf_forensics")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
