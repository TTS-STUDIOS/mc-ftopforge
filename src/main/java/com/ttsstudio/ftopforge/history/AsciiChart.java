package com.ttsstudio.ftopforge.history;

import com.ttsstudio.ftopforge.util.MoneyFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AsciiChart {
    private static final int COLS = 12;
    private static final int ROWS = 8;

    private AsciiChart() {}

    public static List<String> render(String factionName, List<HistoryPoint> points, int weeks) {
        List<String> out = new ArrayList<>();
        if (points == null || points.isEmpty()) {
            out.add("&7No history to show for &b" + factionName + "&7.");
            return out;
        }
        // Header
        out.add("&6&l=== " + factionName + " — últimas " + weeks + " semanas ===");

        // Series may arrive ORDERED BY finished_at DESC (from listFor). Build chronological asc.
        List<HistoryPoint> asc = new ArrayList<>(points);
        Collections.reverse(asc);

        long minTs = asc.get(0).finishedAt();
        long maxTs = asc.get(asc.size() - 1).finishedAt();
        long range = Math.max(maxTs - minTs, 1L);

        long[] bucketed = new long[COLS];
        boolean[] hasData = new boolean[COLS];
        long[] bucketTs = new long[COLS];
        for (HistoryPoint p : asc) {
            int b = (int) (((p.finishedAt() - minTs) * (COLS - 1)) / range);
            if (b < 0) b = 0;
            if (b >= COLS) b = COLS - 1;
            if (!hasData[b] || p.finishedAt() >= bucketTs[b]) {
                bucketed[b] = p.totalWorth();
                bucketTs[b] = p.finishedAt();
                hasData[b] = true;
            }
        }

        // Compute Y range
        long yMin = Long.MAX_VALUE, yMax = Long.MIN_VALUE;
        for (int i = 0; i < COLS; i++) {
            if (hasData[i]) {
                yMin = Math.min(yMin, bucketed[i]);
                yMax = Math.max(yMax, bucketed[i]);
            }
        }
        if (yMin == Long.MAX_VALUE) { yMin = 0L; yMax = 1L; }
        if (yMin == yMax) { yMax = yMin + 1L; }

        // Render rows top-to-bottom
        for (int r = 0; r < ROWS; r++) {
            double rowFrac = 1.0 - ((double) r / (ROWS - 1));
            long rowVal = yMin + (long) (rowFrac * (yMax - yMin));
            StringBuilder sb = new StringBuilder("&7   ").append(MoneyFormat.shortFmt(rowVal)).append(" ┤");
            for (int c = 0; c < COLS; c++) {
                if (!hasData[c]) { sb.append(" "); continue; }
                double pointFrac = (double) (bucketed[c] - yMin) / (yMax - yMin);
                int pointRow = (int) Math.round((1.0 - pointFrac) * (ROWS - 1));
                sb.append(pointRow == r ? "&e●&7" : " ");
            }
            out.add(sb.toString());
        }

        // X axis baseline
        out.add("&7        ┴─────────────────");

        // Footer
        long first = 0L, last = 0L;
        for (int i = 0; i < COLS; i++) if (hasData[i]) { first = bucketed[i]; break; }
        for (int i = COLS - 1; i >= 0; i--) if (hasData[i]) { last = bucketed[i]; break; }
        long delta = last - first;
        String sign = delta >= 0 ? "+" : "";
        int pct = first == 0 ? 0 : (int) Math.round((double) delta / first * 100);
        out.add("&7Pico: &e" + MoneyFormat.shortFmt(yMax) +
                " &7· Min: &e" + MoneyFormat.shortFmt(yMin) +
                " &7· Δ: " + (delta >= 0 ? "&a" : "&c") + sign + pct + "%");

        return out;
    }
}
