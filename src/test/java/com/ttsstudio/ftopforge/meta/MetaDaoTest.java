package com.ttsstudio.ftopforge.meta;

import com.ttsstudio.ftopforge.storage.Dialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MetaDaoTest {
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
    void getMissing_returnsEmpty() throws Exception {
        assertEquals(Optional.empty(), dao.get("nope"));
    }

    @Test
    void putThenGet_returnsValue() throws Exception {
        dao.put("foo", "bar");
        assertEquals(Optional.of("bar"), dao.get("foo"));
    }

    @Test
    void putExisting_upsertsValue() throws Exception {
        dao.put("k", "v1");
        dao.put("k", "v2");
        assertEquals(Optional.of("v2"), dao.get("k"));
    }
}
