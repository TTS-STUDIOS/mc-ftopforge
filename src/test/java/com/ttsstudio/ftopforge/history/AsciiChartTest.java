package com.ttsstudio.ftopforge.history;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AsciiChartTest {

    private static HistoryPoint pt(long ts, long worth) {
        return new HistoryPoint(1L, ts, "f", "F", 1, worth, 0L, 0L, 0L, 0L, 0L);
    }

    @Test
    void emptySeries_returnsEmptyMessage() {
        List<String> out = AsciiChart.render("Dragones", Collections.emptyList(), 4);
        assertTrue(out.stream().anyMatch(s -> s.contains("No history")));
    }

    @Test
    void singlePoint_doesNotCrash() {
        List<HistoryPoint> series = Collections.singletonList(pt(1000L, 500L));
        List<String> out = AsciiChart.render("F", series, 1);
        assertNotNull(out);
        assertTrue(out.size() >= 2);
    }

    @Test
    void allEqualSeries_doesNotCrash() {
        List<HistoryPoint> series = new ArrayList<>();
        for (int i = 0; i < 5; i++) series.add(pt(1000L + i * 100, 500L));
        List<String> out = AsciiChart.render("F", series, 1);
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    void bucketing_compresses12PointsInto12Columns() {
        List<HistoryPoint> series = new ArrayList<>();
        for (int i = 0; i < 12; i++) series.add(pt(1000L + i * 1000L, 100L + i * 100L));
        List<String> out = AsciiChart.render("F", series, 4);
        assertTrue(out.stream().anyMatch(s -> s.contains("●")));
    }

    @Test
    void footerIncludesPeakMinAndDelta() {
        List<HistoryPoint> series = Arrays.asList(pt(100L, 200L), pt(200L, 400L), pt(300L, 800L));
        List<String> out = AsciiChart.render("F", series, 4);
        String joined = String.join("\n", out);
        assertTrue(joined.contains("Pico"));
        assertTrue(joined.contains("Min"));
        assertTrue(out.get(out.size() - 1).contains("Δ") || out.get(out.size() - 1).contains("%"));
    }
}
