package com.ttsstudio.ftopforge.holograms;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HologramLineFormatterTest {

    static class Stub implements HologramLineFormatter.FactionLike {
        final String name, leader; final long worth;
        Stub(String n, long w, String l) { name = n; worth = w; leader = l; }
        @Override public String name() { return name; }
        @Override public long totalValue() { return worth; }
        @Override public String leaderName() { return leader; }
    }

    @Test
    void resolvesTopNNameAndValueAndLeader() {
        List<Stub> top = Arrays.asList(new Stub("Dragones", 45_200_000L, "LeaderA"));
        String r = HologramLineFormatter.format("&e#1 &b%top1_name% &7- &6$%top1_value% leader=%top1_leader%", top, 1000L);
        assertTrue(r.contains("Dragones"));
        assertTrue(r.contains("LeaderA"));
        // ChatColor translation: "&e" -> "§e"
        assertTrue(r.startsWith("§e"));
    }

    @Test
    void outOfBoundsTopN_rendersDashDash() {
        String r = HologramLineFormatter.format("%top1_name%", Collections.emptyList(), 1000L);
        assertEquals("--", r);
    }

    @Test
    void chatColorAmpersandTranslated() {
        String r = HologramLineFormatter.format("&6Top", Collections.emptyList(), 0L);
        assertTrue(r.startsWith("§6"));
    }

    @Test
    void nextRecalcRendersHumanFormat() {
        // 1390 seconds = 23m 10s
        String r = HologramLineFormatter.format("%next_recalc%", Collections.emptyList(), 1390L);
        assertTrue(r.contains("23m"));
        assertTrue(r.contains("10s"));
    }
}
