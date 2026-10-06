package com.ttsstudio.ftopforge.storage;

public enum Dialect {
    SQLITE {
        @Override public String recalcsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_recalcs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "started_at BIGINT NOT NULL, " +
                    "finished_at BIGINT, " +
                    "duration_ms INTEGER, " +
                    "faction_count INTEGER)";
        }
        @Override public String snapshotsCurrentDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_snapshots_current (" +
                    "faction_id TEXT PRIMARY KEY, " +
                    "faction_name TEXT NOT NULL, " +
                    "leader_uuid TEXT, leader_name TEXT, " +
                    "total_value BIGINT NOT NULL, " +
                    "chunks_value BIGINT, spawners_value BIGINT, items_value BIGINT, " +
                    "blocks_value BIGINT, balance_value BIGINT, " +
                    "chunks_count INTEGER, " +
                    "spawners_count_json TEXT, blocks_count_json TEXT, " +
                    "richest_member_uuid TEXT, richest_member_balance BIGINT, " +
                    "calculated_at BIGINT NOT NULL, " +
                    "recalc_id INTEGER)";
        }
        @Override public String skullCacheDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_skull_cache (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "profile_property_value TEXT NOT NULL, " +
                    "fetched_at BIGINT NOT NULL)";
        }
        @Override public String skullCacheIndexDdl() {
            return "CREATE INDEX IF NOT EXISTS idx_ftf_skull_cache_fetched_at " +
                    "ON ftf_skull_cache(fetched_at)";
        }
        @Override public String historyDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_history (" +
                    "recalc_id INTEGER NOT NULL, " +
                    "finished_at BIGINT NOT NULL, " +
                    "faction_id TEXT NOT NULL, " +
                    "faction_name TEXT NOT NULL, " +
                    "rank INTEGER NOT NULL, " +
                    "total_worth BIGINT NOT NULL, " +
                    "chunks_value BIGINT NOT NULL, " +
                    "spawners_value BIGINT NOT NULL, " +
                    "items_value BIGINT NOT NULL, " +
                    "blocks_value BIGINT NOT NULL, " +
                    "balance BIGINT NOT NULL, " +
                    "PRIMARY KEY (recalc_id, faction_id))";
        }
        @Override public java.util.List<String> historyIndexesDdl() {
            return java.util.Arrays.asList(
                "CREATE INDEX IF NOT EXISTS idx_ftf_history_finished ON ftf_history(finished_at)",
                "CREATE INDEX IF NOT EXISTS idx_ftf_history_faction ON ftf_history(faction_id, finished_at)"
            );
        }
        @Override public String forensicsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_forensics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ts BIGINT NOT NULL, " +
                    "player_uuid TEXT NOT NULL, " +
                    "player_name TEXT NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "material TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "chunk_x INTEGER, chunk_z INTEGER, " +
                    "territory_faction_id TEXT, " +
                    "is_member INTEGER NOT NULL)";
        }
        @Override public java.util.List<String> forensicsIndexesDdl() {
            return java.util.Arrays.asList(
                "CREATE INDEX IF NOT EXISTS idx_ftf_forensics_faction ON ftf_forensics(territory_faction_id, ts)",
                "CREATE INDEX IF NOT EXISTS idx_ftf_forensics_chunk ON ftf_forensics(world, chunk_x, chunk_z, ts)",
                "CREATE INDEX IF NOT EXISTS idx_ftf_forensics_player ON ftf_forensics(player_uuid, ts)"
            );
        }
        @Override public String seasonsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_seasons (" +
                    "season_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "started_at BIGINT NOT NULL, " +
                    "ended_at BIGINT NOT NULL, " +
                    "top10_json TEXT NOT NULL, " +
                    "payouts_log TEXT NOT NULL)";
        }
        @Override public java.util.List<String> seasonsIndexesDdl() {
            return java.util.Collections.singletonList(
                "CREATE INDEX IF NOT EXISTS idx_ftf_seasons_ended ON ftf_seasons(ended_at)"
            );
        }
        @Override public String metaDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_meta (" +
                    "meta_key TEXT PRIMARY KEY, " +
                    "meta_value TEXT NOT NULL)";
        }
    },
    MYSQL {
        @Override public String recalcsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_recalcs (" +
                    "id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                    "started_at BIGINT NOT NULL, " +
                    "finished_at BIGINT, " +
                    "duration_ms INT, " +
                    "faction_count INT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String snapshotsCurrentDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_snapshots_current (" +
                    "faction_id VARCHAR(64) PRIMARY KEY, " +
                    "faction_name VARCHAR(64) NOT NULL, " +
                    "leader_uuid VARCHAR(36), leader_name VARCHAR(64), " +
                    "total_value BIGINT NOT NULL, " +
                    "chunks_value BIGINT, spawners_value BIGINT, items_value BIGINT, " +
                    "blocks_value BIGINT, balance_value BIGINT, " +
                    "chunks_count INT, " +
                    "spawners_count_json TEXT, blocks_count_json TEXT, " +
                    "richest_member_uuid VARCHAR(36), richest_member_balance BIGINT, " +
                    "calculated_at BIGINT NOT NULL, " +
                    "recalc_id INT UNSIGNED) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String skullCacheDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_skull_cache (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "profile_property_value TEXT NOT NULL, " +
                    "fetched_at BIGINT NOT NULL, " +
                    "INDEX idx_ftf_skull_cache_fetched_at (fetched_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String skullCacheIndexDdl() {
            return ""; // MySQL has INDEX inline in the CREATE TABLE above
        }
        @Override public String historyDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_history (" +
                    "recalc_id INT UNSIGNED NOT NULL, " +
                    "finished_at BIGINT NOT NULL, " +
                    "faction_id VARCHAR(64) NOT NULL, " +
                    "faction_name VARCHAR(64) NOT NULL, " +
                    "`rank` INT NOT NULL, " +
                    "total_worth BIGINT NOT NULL, " +
                    "chunks_value BIGINT NOT NULL, " +
                    "spawners_value BIGINT NOT NULL, " +
                    "items_value BIGINT NOT NULL, " +
                    "blocks_value BIGINT NOT NULL, " +
                    "balance BIGINT NOT NULL, " +
                    "PRIMARY KEY (recalc_id, faction_id), " +
                    "INDEX idx_ftf_history_finished (finished_at), " +
                    "INDEX idx_ftf_history_faction (faction_id, finished_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public java.util.List<String> historyIndexesDdl() {
            return java.util.Collections.emptyList();
        }
        @Override public String forensicsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_forensics (" +
                    "id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                    "ts BIGINT NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(32) NOT NULL, " +
                    "action VARCHAR(8) NOT NULL, " +
                    "material VARCHAR(64) NOT NULL, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x INT, y INT, z INT, " +
                    "chunk_x INT, chunk_z INT, " +
                    "territory_faction_id VARCHAR(64), " +
                    "is_member TINYINT(1) NOT NULL, " +
                    "INDEX idx_ftf_forensics_faction (territory_faction_id, ts), " +
                    "INDEX idx_ftf_forensics_chunk (world, chunk_x, chunk_z, ts), " +
                    "INDEX idx_ftf_forensics_player (player_uuid, ts)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public java.util.List<String> forensicsIndexesDdl() {
            return java.util.Collections.emptyList();
        }
        @Override public String seasonsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_seasons (" +
                    "season_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                    "started_at BIGINT NOT NULL, " +
                    "ended_at BIGINT NOT NULL, " +
                    "top10_json TEXT NOT NULL, " +
                    "payouts_log TEXT NOT NULL, " +
                    "INDEX idx_ftf_seasons_ended (ended_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public java.util.List<String> seasonsIndexesDdl() {
            return java.util.Collections.emptyList();
        }
        @Override public String metaDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_meta (" +
                    "meta_key VARCHAR(64) PRIMARY KEY, " +
                    "meta_value VARCHAR(512) NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
    };

    public abstract String recalcsDdl();
    public abstract String snapshotsCurrentDdl();
    public abstract String skullCacheDdl();
    public abstract String skullCacheIndexDdl();
    public abstract String historyDdl();
    public abstract java.util.List<String> historyIndexesDdl();
    public abstract String forensicsDdl();
    public abstract java.util.List<String> forensicsIndexesDdl();
    public abstract String seasonsDdl();
    public abstract java.util.List<String> seasonsIndexesDdl();
    public abstract String metaDdl();

    public static Dialect byName(String name) {
        if (name == null) throw new IllegalArgumentException("storage.type is null");
        switch (name.toLowerCase()) {
            case "sqlite": return SQLITE;
            case "mysql": return MYSQL;
            default: throw new IllegalArgumentException("Unknown storage type: " + name);
        }
    }
}
