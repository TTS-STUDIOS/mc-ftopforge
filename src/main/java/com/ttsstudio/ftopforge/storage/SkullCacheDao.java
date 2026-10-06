package com.ttsstudio.ftopforge.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkullCacheDao {

    private final JdbcRepository repo;

    public SkullCacheDao(JdbcRepository repo) {
        this.repo = repo;
    }

    public SkullCacheRow get(UUID uuid) throws SQLException {
        String sql = "SELECT uuid, profile_property_value, fetched_at FROM ftf_skull_cache WHERE uuid = ?";
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new SkullCacheRow(
                        UUID.fromString(rs.getString(1)),
                        rs.getString(2),
                        rs.getLong(3));
            }
        }
    }

    public void put(UUID uuid, String textureValue, long fetchedAt) throws SQLException {
        // Portable upsert via DELETE + INSERT inside a transaction
        // (sqlite supports ON CONFLICT but MySQL syntax differs — single-writer pool makes this safe).
        String del = "DELETE FROM ftf_skull_cache WHERE uuid = ?";
        String ins = "INSERT INTO ftf_skull_cache (uuid, profile_property_value, fetched_at) VALUES (?, ?, ?)";
        try (Connection c = repo.connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement d = c.prepareStatement(del)) {
                d.setString(1, uuid.toString());
                d.executeUpdate();
            }
            try (PreparedStatement i = c.prepareStatement(ins)) {
                i.setString(1, uuid.toString());
                i.setString(2, textureValue);
                i.setLong(3, fetchedAt);
                i.executeUpdate();
            }
            c.commit();
            c.setAutoCommit(true);
        }
    }

    public int deleteExpired(long olderThanFetchedAt) throws SQLException {
        String sql = "DELETE FROM ftf_skull_cache WHERE fetched_at < ?";
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, olderThanFetchedAt);
            return ps.executeUpdate();
        }
    }

    public List<SkullCacheRow> loadWarmup(int limit) throws SQLException {
        String sql = "SELECT uuid, profile_property_value, fetched_at FROM ftf_skull_cache " +
                     "ORDER BY fetched_at DESC LIMIT ?";
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<SkullCacheRow> out = new ArrayList<SkullCacheRow>();
                while (rs.next()) {
                    out.add(new SkullCacheRow(
                            UUID.fromString(rs.getString(1)),
                            rs.getString(2),
                            rs.getLong(3)));
                }
                return out;
            }
        }
    }

    public void ensureSchema() throws SQLException {
        try (Connection c = repo.connection();
             java.sql.Statement st = c.createStatement()) {
            st.execute(repo.dialect().skullCacheDdl());
            String idx = repo.dialect().skullCacheIndexDdl();
            if (idx != null && !idx.isEmpty()) {
                st.execute(idx);
            }
        }
    }
}
