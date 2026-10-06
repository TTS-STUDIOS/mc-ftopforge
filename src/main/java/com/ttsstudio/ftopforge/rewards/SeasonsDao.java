package com.ttsstudio.ftopforge.rewards;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SeasonsDao {
    private final DataSource ds;

    public SeasonsDao(DataSource ds) { this.ds = ds; }

    public long insertSeason(SeasonRow row) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO ftf_seasons(started_at, ended_at, top10_json, payouts_log) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, row.startedAt());
            ps.setLong(2, row.endedAt());
            ps.setString(3, row.top10Json());
            ps.setString(4, row.payoutsLog());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        }
    }

    public List<SeasonRow> list(int page, int pageSize) throws SQLException {
        List<SeasonRow> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT season_id, started_at, ended_at, top10_json, payouts_log " +
                 "FROM ftf_seasons ORDER BY ended_at DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, pageSize);
            ps.setInt(2, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SeasonRow(
                        rs.getLong(1), rs.getLong(2), rs.getLong(3),
                        rs.getString(4), rs.getString(5)));
                }
            }
        }
        return out;
    }
}
