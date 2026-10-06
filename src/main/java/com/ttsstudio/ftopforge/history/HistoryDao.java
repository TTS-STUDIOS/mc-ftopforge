package com.ttsstudio.ftopforge.history;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class HistoryDao {
    private static final String INSERT_SQL =
        "INSERT INTO ftf_history(recalc_id, finished_at, faction_id, faction_name, `rank`, " +
        "total_worth, chunks_value, spawners_value, items_value, blocks_value, balance) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_SQL =
        "SELECT recalc_id, finished_at, faction_id, faction_name, `rank`, " +
        "total_worth, chunks_value, spawners_value, items_value, blocks_value, balance " +
        "FROM ftf_history WHERE faction_id = ? AND finished_at BETWEEN ? AND ? " +
        "ORDER BY finished_at DESC";

    private final DataSource ds;

    public HistoryDao(DataSource ds) { this.ds = ds; }

    public void insertTop10(List<HistoryPoint> top) throws SQLException {
        if (top == null || top.isEmpty()) return;
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
                for (HistoryPoint p : top) {
                    ps.setLong(1, p.recalcId());
                    ps.setLong(2, p.finishedAt());
                    ps.setString(3, p.factionId());
                    ps.setString(4, p.factionName());
                    ps.setInt(5, p.rank());
                    ps.setLong(6, p.totalWorth());
                    ps.setLong(7, p.chunksValue());
                    ps.setLong(8, p.spawnersValue());
                    ps.setLong(9, p.itemsValue());
                    ps.setLong(10, p.blocksValue());
                    ps.setLong(11, p.balance());
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public List<HistoryPoint> listFor(String factionId, long fromMs, long toMs) throws SQLException {
        List<HistoryPoint> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_SQL)) {
            ps.setString(1, factionId);
            ps.setLong(2, fromMs);
            ps.setLong(3, toMs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new HistoryPoint(
                        rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getLong(6), rs.getLong(7), rs.getLong(8),
                        rs.getLong(9), rs.getLong(10), rs.getLong(11)));
                }
            }
        }
        return out;
    }

    public int deleteOlderThan(long cutoffMs) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM ftf_history WHERE finished_at < ?")) {
            ps.setLong(1, cutoffMs);
            return ps.executeUpdate();
        }
    }
}
