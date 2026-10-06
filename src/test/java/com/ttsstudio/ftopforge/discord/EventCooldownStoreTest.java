package com.ttsstudio.ftopforge.discord;

import com.ttsstudio.ftopforge.meta.MetaDao;
import com.ttsstudio.ftopforge.storage.Dialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class EventCooldownStoreTest {
    private HikariDataSource ds;
    private MetaDao dao;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(Dialect.SQLITE.metaDdl());
        }
        dao = new MetaDao(ds);
    }

    @AfterEach
    void tearDown() { ds.close(); }

    @Test
    void withinCooldown_skipsSecondFire() throws Exception {
        EventCooldownStore store = new EventCooldownStore(dao);
        long now = 1_000_000L;
        assertTrue(store.tryFire("on-top1-changed", 6, now));
        assertFalse(store.tryFire("on-top1-changed", 6, now + 1_000L));
    }

    @Test
    void afterCooldownElapsed_firesAgain() throws Exception {
        EventCooldownStore store = new EventCooldownStore(dao);
        long now = 1_000_000L;
        store.tryFire("evt", 6, now);
        long later = now + 7L * 3_600_000L;
        assertTrue(store.tryFire("evt", 6, later));
    }
}
