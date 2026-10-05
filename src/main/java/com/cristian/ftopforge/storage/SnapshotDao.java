package com.cristian.ftopforge.storage;

import com.cristian.ftopforge.core.FactionSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class SnapshotDao {

    private final JdbcRepository repo;

    public SnapshotDao(JdbcRepository repo) {
        this.repo = repo;
    }

    public void upsert(FactionSnapshot s, long recalcId) throws SQLException {
        String upsert = repo.dialect() == Dialect.SQLITE
                ? "INSERT OR REPLACE INTO ftf_snapshots_current" +
                  "(faction_id, faction_name, leader_uuid, leader_name, total_value, " +
                  " chunks_value, spawners_value, items_value, blocks_value, balance_value, " +
                  " chunks_count, spawners_count_json, blocks_count_json, " +
                  " richest_member_uuid, richest_member_balance, calculated_at, recalc_id) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                : "INSERT INTO ftf_snapshots_current" +
                  "(faction_id, faction_name, leader_uuid, leader_name, total_value, " +
                  " chunks_value, spawners_value, items_value, blocks_value, balance_value, " +
                  " chunks_count, spawners_count_json, blocks_count_json, " +
                  " richest_member_uuid, richest_member_balance, calculated_at, recalc_id) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE faction_name=VALUES(faction_name), leader_uuid=VALUES(leader_uuid), " +
                  "leader_name=VALUES(leader_name), total_value=VALUES(total_value), " +
                  "chunks_value=VALUES(chunks_value), spawners_value=VALUES(spawners_value), " +
                  "items_value=VALUES(items_value), blocks_value=VALUES(blocks_value), " +
                  "balance_value=VALUES(balance_value), chunks_count=VALUES(chunks_count), " +
                  "spawners_count_json=VALUES(spawners_count_json), blocks_count_json=VALUES(blocks_count_json), " +
                  "richest_member_uuid=VALUES(richest_member_uuid), richest_member_balance=VALUES(richest_member_balance), " +
                  "calculated_at=VALUES(calculated_at), recalc_id=VALUES(recalc_id)";

        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, s.factionId());
            ps.setString(2, s.factionName());
            ps.setString(3, s.leaderUuid());
            ps.setString(4, s.leaderName());
            ps.setLong(5, s.totalValue());
            ps.setLong(6, s.chunksValue());
            ps.setLong(7, s.spawnersValue());
            ps.setLong(8, s.itemsValue());
            ps.setLong(9, s.blocksValue());
            ps.setLong(10, s.balanceValue());
            ps.setInt(11, s.chunksCount());
            ps.setString(12, mapToJson(s.spawnersCount()));
            ps.setString(13, mapToJson(s.blocksCount()));
            ps.setString(14, s.richestMemberUuid());
            ps.setLong(15, s.richestMemberBalance());
            ps.setLong(16, s.calculatedAt());
            ps.setLong(17, recalcId);
            ps.executeUpdate();
        }
    }

    /** Minimal hand-rolled JSON serializer for Map<String, Integer> (no Gson dep). */
    /**
     * The rows written by the newest recalculation. Older rows belong to
     * factions that no longer exist and are left out.
     */
    public java.util.List<FactionSnapshot> loadLatest() throws SQLException {
        java.util.List<FactionSnapshot> out = new java.util.ArrayList<>();
        String sql = "SELECT faction_id, faction_name, leader_uuid, leader_name, chunks_value, "
            + "spawners_value, items_value, blocks_value, balance_value, chunks_count, "
            + "spawners_count_json, blocks_count_json, richest_member_uuid, richest_member_balance, "
            + "calculated_at FROM ftf_snapshots_current "
            + "WHERE recalc_id = (SELECT MAX(recalc_id) FROM ftf_snapshots_current)";
        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FactionSnapshot.Builder b = FactionSnapshot.builder(rs.getString(1), rs.getString(2))
                    .leader(rs.getString(3), rs.getString(4))
                    .addChunksValue(rs.getLong(5)).addSpawnersValue(rs.getLong(6))
                    .addItemsValue(rs.getLong(7)).addBlocksValue(rs.getLong(8))
                    .addBalanceValue(rs.getLong(9)).chunksCount(rs.getInt(10))
                    .richest(rs.getString(13), rs.getLong(14)).calculatedAt(rs.getLong(15));
                for (Map.Entry<String, Integer> e : jsonToMap(rs.getString(11)).entrySet()) b.spawnerCount(e.getKey(), e.getValue());
                for (Map.Entry<String, Integer> e : jsonToMap(rs.getString(12)).entrySet()) b.blockCount(e.getKey(), e.getValue());
                out.add(b.build());
            }
        }
        return out;
    }

    private static final java.util.regex.Pattern PAIR =
        java.util.regex.Pattern.compile("\"([^\"]+)\":(\\d+)");

    static Map<String, Integer> jsonToMap(String json) {
        Map<String, Integer> out = new java.util.HashMap<>();
        if (json == null) return out;
        java.util.regex.Matcher m = PAIR.matcher(json);
        while (m.find()) {
            try { out.put(m.group(1), Integer.parseInt(m.group(2))); } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    private static String mapToJson(Map<String, Integer> m) {
        if (m == null || m.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey().replace("\"", "\\\"")).append("\":").append(e.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
