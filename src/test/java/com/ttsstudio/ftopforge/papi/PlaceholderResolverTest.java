package com.ttsstudio.ftopforge.papi;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderResolverTest {

    static class Stub implements PlaceholderResolver.FactionLike {
        final String id, name, leader; final long worth;
        Stub(String id, String name, long worth, String leader) {
            this.id = id; this.name = name; this.worth = worth; this.leader = leader;
        }
        @Override public String factionId() { return id; }
        @Override public String factionName() { return name; }
        @Override public long totalValue() { return worth; }
        @Override public String leaderName() { return leader; }
    }

    private PlaceholderResolver mkResolver(List<? extends PlaceholderResolver.FactionLike> top,
                                           long secsUntilRecalc, long lastRecalcAgoSecs,
                                           java.util.function.Function<UUID, String> playerToFaction) {
        return new PlaceholderResolver(() -> top, () -> secsUntilRecalc,
            () -> lastRecalcAgoSecs, playerToFaction);
    }

    @Test
    void top1Name_returnsTop1FactionName() {
        PlaceholderResolver r = mkResolver(
            Collections.singletonList(new Stub("f1", "Dragones", 1_000_000L, "L1")),
            0L, 0L, uuid -> null);
        assertEquals("Dragones", r.resolve(null, "top1_name"));
    }

    @Test
    void top5Name_outOfBounds_returnsEmpty() {
        PlaceholderResolver r = mkResolver(
            Collections.singletonList(new Stub("f1", "F1", 1L, null)),
            0L, 0L, uuid -> null);
        assertEquals("", r.resolve(null, "top5_name"));
    }

    @Test
    void rankWithoutFaction_returnsDash() {
        UUID u = UUID.randomUUID();
        PlaceholderResolver r = mkResolver(
            Collections.emptyList(), 0L, 0L, uuid -> null);
        assertEquals("—", r.resolve(u, "rank"));
    }

    @Test
    void value_formattedWithMoneyShortFmt() {
        PlaceholderResolver r = mkResolver(
            Collections.singletonList(new Stub("f1", "F1", 1_200_000L, null)),
            0L, 0L, uuid -> null);
        String out = r.resolve(null, "top1_value");
        assertTrue(out.contains("M") || out.contains("1.2"));
    }

    @Test
    void nextRecalc_returnsHumanFormat() {
        PlaceholderResolver r = mkResolver(
            Collections.emptyList(), 1390L, 0L, uuid -> null);
        String out = r.resolve(null, "next_recalc");
        assertTrue(out.contains("23m"));
        assertTrue(out.contains("10s"));
    }

    @Test
    void factionsTotal_returnsTopSize() {
        PlaceholderResolver r = mkResolver(
            Arrays.asList(new Stub("a","A",1L,null), new Stub("b","B",2L,null)),
            0L, 0L, uuid -> null);
        assertEquals("2", r.resolve(null, "factions_total"));
    }
}
