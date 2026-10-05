package com.cristian.ftopforge.rewards;

import com.cristian.ftopforge.core.FactionSnapshot;
import com.cristian.ftopforge.util.MoneyFormat;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PayoutRunner {

    public interface CommandDispatcher { void dispatch(String command); }
    public interface Broadcaster { void broadcast(String message); }

    private final SeasonService seasonService;
    private final Map<Integer, List<String>> rankCommands;
    private final boolean broadcastEnabled;
    private final String broadcastTemplate;
    private final CommandDispatcher dispatcher;
    private final Broadcaster broadcaster;
    private final Runnable freezer;
    private final Runnable unfreezer;
    private final Logger log;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    public PayoutRunner(SeasonService seasonService,
                        Map<Integer, List<String>> rankCommands,
                        boolean broadcastEnabled, String broadcastTemplate,
                        CommandDispatcher dispatcher, Broadcaster broadcaster,
                        Runnable freezer, Runnable unfreezer, Logger log) {
        this.seasonService = seasonService;
        this.rankCommands = rankCommands;
        this.broadcastEnabled = broadcastEnabled;
        this.broadcastTemplate = broadcastTemplate;
        this.dispatcher = dispatcher;
        this.broadcaster = broadcaster;
        this.freezer = freezer != null ? freezer : new Runnable() { public void run() {} };
        this.unfreezer = unfreezer != null ? unfreezer : new Runnable() { public void run() {} };
        this.log = log;
    }

    public boolean isInProgress() { return inProgress.get(); }

    public boolean run(List<FactionSnapshot> top, long now) {
        // An empty ranking (first minutes after a restart) pays nobody: recording
        // it would mark the week as paid and the rewards would be lost.
        if (top == null || top.isEmpty()) {
            if (log != null) log.warning("[payout] the ranking is empty: nothing paid, nothing recorded");
            return false;
        }
        if (!inProgress.compareAndSet(false, true)) return false;
        try {
            freezer.run();
            StringBuilder payoutsLog = new StringBuilder();
            for (Map.Entry<Integer, List<String>> e : rankCommands.entrySet()) {
                int rank = e.getKey();
                if (rank > top.size()) {
                    payoutsLog.append("SKIP rank ").append(rank)
                              .append(" (top has only ").append(top.size()).append(")\n");
                    continue;
                }
                FactionSnapshot s = top.get(rank - 1);
                for (String cmd : e.getValue()) {
                    String resolved = cmd
                        .replace("%faction_name%", s.factionName())
                        .replace("%faction_leader_name%", s.leaderName() != null ? s.leaderName() : "Unknown")
                        .replace("%faction_value%", MoneyFormat.shortFmt(s.totalValue()));
                    try {
                        dispatcher.dispatch(resolved);
                        payoutsLog.append("OK ").append(resolved).append('\n');
                    } catch (Throwable t) {
                        payoutsLog.append("FAIL ").append(resolved).append(" :: ").append(t.getMessage()).append('\n');
                        if (log != null) log.log(Level.WARNING, "[payout] dispatch failed: " + resolved, t);
                    }
                }
            }
            String top10Json = serializeTop10(top);
            try {
                seasonService.recordPayout(now, top10Json, payoutsLog.toString());
            } catch (SQLException ex) {
                if (log != null) log.log(Level.WARNING, "[payout] recordPayout failed", ex);
            }
            if (broadcastEnabled && !top.isEmpty() && broadcastTemplate != null) {
                FactionSnapshot t1 = top.get(0);
                String msg = broadcastTemplate
                    .replace("%faction%", t1.factionName())
                    .replace("%value%", MoneyFormat.shortFmt(t1.totalValue()));
                broadcaster.broadcast(msg);
            }
            return true;
        } finally {
            try { unfreezer.run(); } catch (Throwable ignored) {}
            inProgress.set(false);
        }
    }

    private static String serializeTop10(List<FactionSnapshot> top) {
        StringBuilder sb = new StringBuilder("[");
        int limit = Math.min(10, top.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            FactionSnapshot s = top.get(i);
            sb.append("{\"rank\":").append(i + 1)
              .append(",\"faction_id\":\"").append(esc(s.factionId())).append("\"")
              .append(",\"faction_name\":\"").append(esc(s.factionName())).append("\"")
              .append(",\"total_worth\":").append(s.totalValue()).append("}");
        }
        return sb.append("]").toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
