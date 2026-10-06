package com.ttsstudio.ftopforge.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TopCacheTest {

    private FactionSnapshot snap(String id, long total) {
        // Use the Builder, splitting `total` arbitrarily across slots
        return FactionSnapshot.builder(id, id + "_name")
                .leader("uuid-" + id, "leader-" + id)
                .addChunksValue(total)
                .build();
    }

    @Test
    void rebuildSortsDescByTotalValue() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100), snap("b", 300), snap("c", 200)));
        List<FactionSnapshot> top = cache.top(10);
        assertEquals(3, top.size());
        assertEquals("b", top.get(0).factionId());
        assertEquals("c", top.get(1).factionId());
        assertEquals("a", top.get(2).factionId());
    }

    @Test
    void topNRespectsLimit() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 1), snap("b", 2), snap("c", 3), snap("d", 4)));
        assertEquals(2, cache.top(2).size());
        assertEquals("d", cache.top(2).get(0).factionId());
    }

    @Test
    void rankOfReturnsOneBasedIndexOrZero() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100), snap("b", 300)));
        assertEquals(1, cache.rankOf("b"));
        assertEquals(2, cache.rankOf("a"));
        assertEquals(0, cache.rankOf("c")); // not present
    }

    @Test
    void getByIdReturnsCorrectSnapshot() {
        TopCache cache = new TopCache();
        cache.rebuild(Arrays.asList(snap("a", 100)));
        assertNotNull(cache.byId("a"));
        assertEquals(100, cache.byId("a").totalValue());
        assertNull(cache.byId("nope"));
    }

    @Test
    void topRange_returnsSubList_byInclusiveFromExclusiveTo() {
        TopCache tc = new TopCache();
        List<FactionSnapshot> snaps = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            snaps.add(FactionSnapshot.builder("id-" + i, "name-" + i)
                    .leader("uuid-" + i, "leader-" + i)
                    .addChunksValue(1000 - i)  // descending value
                    .build());
        }
        tc.rebuild(snaps);
        List<FactionSnapshot> page2 = tc.topRange(45, 90);
        assertEquals(5, page2.size()); // 50 total - 45 = 5
        assertEquals("name-45", page2.get(0).factionName());
    }

    @Test
    void topRange_handles_outOfBounds() {
        TopCache tc = new TopCache();
        tc.rebuild(Collections.emptyList());
        assertTrue(tc.topRange(0, 45).isEmpty());
        assertTrue(tc.topRange(45, 90).isEmpty());
    }

    @Test
    void averagesTop10_computesArithmeticMean_overFirstTen() {
        TopCache tc = new TopCache();
        List<FactionSnapshot> snaps = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            snaps.add(FactionSnapshot.builder("id-" + i, "n-" + i)
                    .addChunksValue(100)
                    .addSpawnersValue(200)
                    .addItemsValue(300)
                    .addBlocksValue(400)
                    .addBalanceValue(500)
                    .build());
        }
        tc.rebuild(snaps);
        AvgBreakdown avg = tc.averagesTop10();
        assertEquals(100, avg.chunks);
        assertEquals(200, avg.spawners);
        assertEquals(300, avg.items);
        assertEquals(400, avg.blocks);
        assertEquals(500, avg.balance);
    }

    @Test
    void averagesTop10_usesAvailable_whenLessThanTen() {
        TopCache tc = new TopCache();
        List<FactionSnapshot> snaps = new ArrayList<>();
        snaps.add(FactionSnapshot.builder("a", "A").addChunksValue(100).build());
        snaps.add(FactionSnapshot.builder("b", "B").addChunksValue(300).build());
        tc.rebuild(snaps);
        AvgBreakdown avg = tc.averagesTop10();
        assertEquals(200, avg.chunks); // (100+300)/2
    }

    @Test
    void averagesTop10_zero_whenEmpty() {
        TopCache tc = new TopCache();
        tc.rebuild(Collections.emptyList());
        AvgBreakdown avg = tc.averagesTop10();
        assertEquals(0, avg.chunks);
        assertEquals(0, avg.balance);
    }
}
