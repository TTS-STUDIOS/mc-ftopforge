package com.ttsstudio.ftopforge.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RecalcDao {

    private final JdbcRepository repo;

    public RecalcDao(JdbcRepository repo) {
        this.repo = repo;
    }

    /** Insert a new in-flight recalc row, return its id. */
    public long startNew() throws SQLException {
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO ftf_recalcs(started_at) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("No generated key returned for ftf_recalcs insert");
        }
    }

    public void markFinished(long recalcId, long durationMs, int factionCount) throws SQLException {
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE ftf_recalcs SET finished_at=?, duration_ms=?, faction_count=? WHERE id=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, durationMs);
            ps.setInt(3, factionCount);
            ps.setLong(4, recalcId);
            ps.executeUpdate();
        }
    }

    /** Sets finished_at on any in-flight rows (called on plugin enable to recover from crashes). */
    public int markStaleAsAborted() throws SQLException {
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE ftf_recalcs SET finished_at=?, duration_ms=-1 WHERE finished_at IS NULL")) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        }
    }
}
