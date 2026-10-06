package com.ttsstudio.ftopforge.forensics;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO for the {@code ftf_forensics} table.
 * Inserts are batched via {@link #insertBatch(List)}; queries are paginated
 * via {@link #queryByFaction(String, ForensicsEvent.Action, boolean, int, int)};
 * retention is handled by {@link #deleteOlderThan(long)}.
 */
public class ForensicsDao {

    private final DataSource ds;

    public ForensicsDao(DataSource ds) {
        this.ds = ds;
    }

    public void insertBatch(List<ForensicsEvent> events) throws SQLException {
        if (events == null || events.isEmpty()) return;
        final String sql = "INSERT INTO ftf_forensics(ts, player_uuid, player_name, action, material, " +
            "world, x, y, z, chunk_x, chunk_z, territory_faction_id, is_member) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            try {
                for (ForensicsEvent e : events) {
                    ps.setLong(1, e.ts);
                    ps.setString(2, e.playerUuid.toString());
                    ps.setString(3, e.playerName);
                    ps.setString(4, e.action.name());
                    ps.setString(5, e.material);
                    ps.setString(6, e.world);
                    ps.setInt(7, e.x);
                    ps.setInt(8, e.y);
                    ps.setInt(9, e.z);
                    ps.setInt(10, e.chunkX);
                    ps.setInt(11, e.chunkZ);
                    if (e.territoryFactionId != null) ps.setString(12, e.territoryFactionId);
                    else ps.setNull(12, Types.VARCHAR);
                    ps.setInt(13, e.isMember ? 1 : 0);
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Query events for a faction's territory, ordered by ts DESC.
     *
     * @param factionId    territory faction id (required).
     * @param actionFilter PLACE or BREAK to filter, or null for both.
     * @param insiderOnly  when true, only events by members of the faction that owns the land.
     * @param page         zero-based page index.
     * @param pageSize     rows per page.
     */
    public List<ForensicsEvent> queryByFaction(String factionId, ForensicsEvent.Action actionFilter,
                                               boolean insiderOnly, int page, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT ts, player_uuid, player_name, action, material, world, x, y, z, " +
            "chunk_x, chunk_z, territory_faction_id, is_member " +
            "FROM ftf_forensics WHERE territory_faction_id = ?");
        if (actionFilter != null) sql.append(" AND action = ?");
        if (insiderOnly) sql.append(" AND is_member = 1");
        sql.append(" ORDER BY ts DESC LIMIT ? OFFSET ?");

        List<ForensicsEvent> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setString(i++, factionId);
            if (actionFilter != null) ps.setString(i++, actionFilter.name());
            ps.setInt(i++, pageSize);
            ps.setInt(i, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String terr = rs.getString("territory_faction_id");
                    if (rs.wasNull()) terr = null;
                    out.add(new ForensicsEvent(
                        rs.getLong("ts"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        ForensicsEvent.Action.valueOf(rs.getString("action")),
                        rs.getString("material"),
                        rs.getString("world"),
                        rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                        rs.getInt("chunk_x"), rs.getInt("chunk_z"),
                        terr,
                        rs.getInt("is_member") != 0
                    ));
                }
            }
        }
        return out;
    }

    public int deleteOlderThan(long cutoffMs) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ftf_forensics WHERE ts < ?")) {
            ps.setLong(1, cutoffMs);
            return ps.executeUpdate();
        }
    }
}
