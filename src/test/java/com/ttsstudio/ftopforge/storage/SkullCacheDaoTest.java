package com.ttsstudio.ftopforge.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkullCacheDaoTest {

    private HikariDataSource ds;
    private JdbcRepository repo;
    private SkullCacheDao dao;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("org.sqlite.JDBC");
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        repo = new JdbcRepository(ds, Dialect.SQLITE);
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(Dialect.SQLITE.skullCacheDdl());
            st.execute(Dialect.SQLITE.skullCacheIndexDdl());
        }
        dao = new SkullCacheDao(repo);
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    @Test
    void put_then_get_roundtrip() throws Exception {
        UUID u = UUID.randomUUID();
        dao.put(u, "fake-base64-texture-value", 1000L);
        SkullCacheRow row = dao.get(u);
        assertNotNull(row);
        assertEquals(u, row.uuid);
        assertEquals("fake-base64-texture-value", row.textureValue);
        assertEquals(1000L, row.fetchedAt);
    }

    @Test
    void get_returnsNull_whenAbsent() throws Exception {
        assertNull(dao.get(UUID.randomUUID()));
    }

    @Test
    void put_isUpsert_overwritesOlderRow() throws Exception {
        UUID u = UUID.randomUUID();
        dao.put(u, "v1", 1000L);
        dao.put(u, "v2", 2000L);
        SkullCacheRow row = dao.get(u);
        assertEquals("v2", row.textureValue);
        assertEquals(2000L, row.fetchedAt);
    }

    @Test
    void deleteExpired_removesRows_olderThanCutoff() throws Exception {
        dao.put(UUID.randomUUID(), "old", 1000L);
        dao.put(UUID.randomUUID(), "young", 9000L);
        int deleted = dao.deleteExpired(5000L);
        assertEquals(1, deleted);
    }

    @Test
    void loadWarmup_returnsMostRecentRows_limitedByCount() throws Exception {
        for (int i = 0; i < 10; i++) {
            dao.put(UUID.randomUUID(), "v" + i, (long) i * 100);
        }
        List<SkullCacheRow> rows = dao.loadWarmup(5);
        assertEquals(5, rows.size());
        // sorted desc by fetched_at => first row has fetched_at = 900 (v9)
        assertEquals("v9", rows.get(0).textureValue);
        assertEquals(900L, rows.get(0).fetchedAt);
    }
}
