package com.ttsstudio.ftopforge.rewards;

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

class CatchUpTest {
    private HikariDataSource ds;
    private MetaDao metaDao;
    private SeasonService svc;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(Dialect.SQLITE.metaDdl());
            s.execute(Dialect.SQLITE.seasonsDdl());
            for (String idx : Dialect.SQLITE.seasonsIndexesDdl()) s.execute(idx);
        }
        metaDao = new MetaDao(ds);
        svc = new SeasonService(metaDao, new SeasonsDao(ds));
    }

    @AfterEach
    void tearDown() { ds.close(); }

    @Test
    void firstRun_lastPayoutAtZero_noCatchUp() throws Exception {
        // ftf_meta is empty -> no last_payout_at present
        assertFalse(svc.shouldCatchUp(System.currentTimeMillis()));
        // Explicit 0 also should not trigger
        metaDao.putLong("last_payout_at", 0L);
        assertFalse(svc.shouldCatchUp(System.currentTimeMillis()));
    }

    @Test
    void pastDueByOver7d_triggersCatchUp() throws Exception {
        long now = 100L * 86_400_000L;
        metaDao.putLong("last_payout_at", now - 8L * 86_400_000L);
        assertTrue(svc.shouldCatchUp(now));
    }
}
