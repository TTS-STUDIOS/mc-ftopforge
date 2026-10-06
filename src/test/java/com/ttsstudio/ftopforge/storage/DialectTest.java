package com.ttsstudio.ftopforge.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DialectTest {
    @Test
    void sqliteUsesAutoincrement() {
        String ddl = Dialect.SQLITE.recalcsDdl();
        assertTrue(ddl.contains("AUTOINCREMENT"));
        assertTrue(ddl.contains("ftf_recalcs"));
    }

    @Test
    void mysqlUsesAutoIncrement() {
        String ddl = Dialect.MYSQL.recalcsDdl();
        assertTrue(ddl.contains("AUTO_INCREMENT"));
        assertFalse(ddl.contains("AUTOINCREMENT"));
    }

    @Test
    void factoryByName() {
        assertSame(Dialect.SQLITE, Dialect.byName("sqlite"));
        assertSame(Dialect.SQLITE, Dialect.byName("SQLite"));
        assertSame(Dialect.MYSQL, Dialect.byName("mysql"));
        assertThrows(IllegalArgumentException.class, () -> Dialect.byName("oracle"));
    }

    @Test
    void sqlite_skullCacheDdl_isCreateTableIfNotExists() {
        String ddl = Dialect.SQLITE.skullCacheDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_skull_cache"));
        assertTrue(ddl.contains("uuid TEXT PRIMARY KEY"));
        assertTrue(ddl.contains("profile_property_value TEXT NOT NULL"));
        assertTrue(ddl.contains("fetched_at"));
    }

    @Test
    void mysql_skullCacheDdl_usesVarcharAndInnodb() {
        String ddl = Dialect.MYSQL.skullCacheDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_skull_cache"));
        assertTrue(ddl.contains("uuid VARCHAR(36) PRIMARY KEY"));
        assertTrue(ddl.contains("ENGINE=InnoDB"));
        assertTrue(ddl.contains("CHARSET=utf8mb4"));
    }

    @Test
    void both_dialects_haveFetchedAtIndex() {
        assertTrue(Dialect.SQLITE.skullCacheIndexDdl().contains("idx_ftf_skull_cache_fetched_at"));
        assertTrue(Dialect.MYSQL.skullCacheIndexDdl().contains("idx_ftf_skull_cache_fetched_at") ||
                   Dialect.MYSQL.skullCacheDdl().contains("INDEX idx_ftf_skull_cache_fetched_at"));
    }

    @Test
    void sqlite_historyDdl_hasCompositePkAndIndexes() {
        String ddl = Dialect.SQLITE.historyDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_history"));
        assertTrue(ddl.contains("recalc_id"));
        assertTrue(ddl.contains("PRIMARY KEY (recalc_id, faction_id)"));
        assertTrue(Dialect.SQLITE.historyIndexesDdl().stream()
            .anyMatch(s -> s.contains("idx_ftf_history_finished")));
        assertTrue(Dialect.SQLITE.historyIndexesDdl().stream()
            .anyMatch(s -> s.contains("idx_ftf_history_faction")));
    }

    @Test
    void sqlite_forensicsDdl_hasAutoincrementAndIndexes() {
        String ddl = Dialect.SQLITE.forensicsDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_forensics"));
        assertTrue(ddl.contains("INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(Dialect.SQLITE.forensicsIndexesDdl().size() >= 3);
    }

    @Test
    void both_dialects_haveSeasonsAndMetaDdl() {
        assertTrue(Dialect.SQLITE.seasonsDdl().contains("ftf_seasons"));
        assertTrue(Dialect.MYSQL.seasonsDdl().contains("ftf_seasons"));
        assertTrue(Dialect.SQLITE.metaDdl().contains("ftf_meta"));
        assertTrue(Dialect.MYSQL.metaDdl().contains("ftf_meta"));
        assertTrue(Dialect.SQLITE.metaDdl().contains("meta_key"));
        assertTrue(Dialect.SQLITE.metaDdl().contains("meta_value"));
    }
}
