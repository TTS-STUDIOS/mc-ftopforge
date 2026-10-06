package com.ttsstudio.ftopforge.meta;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class MetaDao {
    private final DataSource ds;

    public MetaDao(DataSource ds) { this.ds = ds; }

    public Optional<String> get(String key) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT meta_value FROM ftf_meta WHERE meta_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString(1));
                return Optional.empty();
            }
        }
    }

    public void put(String key, String value) throws SQLException {
        try (Connection c = ds.getConnection()) {
            // Upsert portable SQLite/MySQL: DELETE + INSERT en transaccion
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement(
                     "DELETE FROM ftf_meta WHERE meta_key = ?");
                 PreparedStatement ins = c.prepareStatement(
                     "INSERT INTO ftf_meta(meta_key, meta_value) VALUES (?, ?)")) {
                del.setString(1, key);
                del.executeUpdate();
                ins.setString(1, key);
                ins.setString(2, value);
                ins.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public Optional<Long> getLong(String key) throws SQLException {
        return get(key).map(Long::parseLong);
    }

    public void putLong(String key, long value) throws SQLException {
        put(key, Long.toString(value));
    }
}
