package com.ttsstudio.ftopforge.history;

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

import static org.junit.jupiter.api.Assertions.*;

class HistoryDaoTest {
    private HikariDataSource ds;
    private HistoryDao dao;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(Dialect.SQLITE.historyDdl());
            for (String idx : Dialect.SQLITE.historyIndexesDdl()) s.execute(idx);
        }
        dao = new HistoryDao(ds);
    }

    @AfterEach
    void tearDown() { ds.close(); }

    @Test
    void insertTop10_inserts10Rows() throws Exception {
        List<HistoryPoint> top = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            top.add(new HistoryPoint(1L, 1000L, "f" + i, "F" + i, i + 1,
                1000L - i * 100, 10L, 20L, 30L, 40L, 50L));
        }
        dao.insertTop10(top);
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ftf_history")) {
            assertTrue(rs.next());
            assertEquals(10, rs.getInt(1));
        }
    }

    @Test
    void insertTop10_withFewerThan10_insertsOnlyAvailable() throws Exception {
        List<HistoryPoint> top = new ArrayList<>();
        top.add(new HistoryPoint(2L, 2000L, "a", "A", 1, 500L, 1L, 2L, 3L, 4L, 5L));
        top.add(new HistoryPoint(2L, 2000L, "b", "B", 2, 400L, 1L, 2L, 3L, 4L, 5L));
        top.add(new HistoryPoint(2L, 2000L, "c", "C", 3, 300L, 1L, 2L, 3L, 4L, 5L));
        dao.insertTop10(top);
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ftf_history")) {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    void listFor_returnsOrderedByFinishedAtDesc() throws Exception {
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(1L, 100L, "f1", "F1", 1, 100L, 0L, 0L, 0L, 0L, 0L)));
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(2L, 200L, "f1", "F1", 1, 200L, 0L, 0L, 0L, 0L, 0L)));
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(3L, 300L, "f1", "F1", 1, 300L, 0L, 0L, 0L, 0L, 0L)));

        List<HistoryPoint> result = dao.listFor("f1", 0L, 1000L);
        assertEquals(3, result.size());
        assertEquals(300L, result.get(0).finishedAt());
        assertEquals(200L, result.get(1).finishedAt());
        assertEquals(100L, result.get(2).finishedAt());
    }

    @Test
    void deleteOlderThan_removesOldRows() throws Exception {
        long now = 100L * 86_400_000L;
        long old = now - 100L * 86_400_000L;
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(1L, old, "f1", "F1", 1, 100L, 0L, 0L, 0L, 0L, 0L)));
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(2L, now, "f1", "F1", 1, 200L, 0L, 0L, 0L, 0L, 0L)));

        int deleted = dao.deleteOlderThan(now - 50L * 86_400_000L);
        assertEquals(1, deleted);

        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ftf_history")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
