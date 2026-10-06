package com.ttsstudio.ftopforge.history;

import com.ttsstudio.ftopforge.core.FactionSnapshot;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class HistoryService {
    private final JavaPlugin plugin;
    private final HistoryDao dao;
    private final int retentionDays;
    private final boolean cleanupOnRecalc;

    public HistoryService(JavaPlugin plugin, HistoryDao dao, int retentionDays, boolean cleanupOnRecalc) {
        this.plugin = plugin;
        this.dao = dao;
        this.retentionDays = retentionDays;
        this.cleanupOnRecalc = cleanupOnRecalc;
    }

    /** Suscriptor a RecalcRunner.FinishCallback. */
    public void onRecalcFinished(long recalcId, long finishedAt, List<FactionSnapshot> orderedTop) {
        if (orderedTop == null || orderedTop.isEmpty()) return;
        List<HistoryPoint> top10 = new ArrayList<>();
        int limit = Math.min(10, orderedTop.size());
        for (int i = 0; i < limit; i++) {
            FactionSnapshot s = orderedTop.get(i);
            top10.add(new HistoryPoint(
                recalcId, finishedAt, s.factionId(), s.factionName(), i + 1,
                s.totalValue(),
                s.chunksValue(), s.spawnersValue(), s.itemsValue(),
                s.blocksValue(), s.balanceValue()));
        }
        try {
            dao.insertTop10(top10);
            if (cleanupOnRecalc) {
                long cutoff = finishedAt - (long) retentionDays * 86_400_000L;
                int deleted = dao.deleteOlderThan(cutoff);
                if (deleted > 0) {
                    plugin.getLogger().fine("[history] retention: " + deleted + " rows older than " + retentionDays + "d deleted");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[history] insert failed", e);
        }
    }
}
