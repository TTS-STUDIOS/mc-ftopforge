package com.ttsstudio.ftopforge.worth;

import com.ttsstudio.ftopforge.core.ItemPrices;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorthQueryTest {

    private static ItemPrices prices(Map<String, Long> items, Map<String, Long> spawners, Map<String, Long> blocks) {
        return new ItemPrices(items, spawners, blocks, 0L, 0L);
    }

    @Test
    void itemNormalLookup_returnsUnitAndTotal() {
        Map<String, Long> blocks = new HashMap<>();
        blocks.put("DIAMOND_BLOCK", 500L);
        ItemPrices p = prices(Collections.emptyMap(), Collections.emptyMap(), blocks);

        WorthQuery.Result r = WorthQuery.compute("DIAMOND_BLOCK", 64, null, p);
        assertEquals(500L, r.unit);
        assertEquals(64 * 500L, r.total);
        assertEquals(64, r.amount);
        assertFalse(r.notListed);
    }

    @Test
    void spawnerLookup_usesEntityType() {
        Map<String, Long> spawners = new HashMap<>();
        spawners.put("PIG", 25_000L);
        ItemPrices p = prices(Collections.emptyMap(), spawners, Collections.emptyMap());

        WorthQuery.Result r = WorthQuery.compute("MOB_SPAWNER", 1, "PIG", p);
        assertEquals(25_000L, r.unit);
        assertEquals(25_000L, r.total);
        assertEquals("PIG_SPAWNER", r.displayMaterial);
    }

    @Test
    void notListedItem_flagSetUnitZero() {
        ItemPrices p = prices(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        WorthQuery.Result r = WorthQuery.compute("STONE", 10, null, p);
        assertEquals(0L, r.unit);
        assertTrue(r.notListed);
    }

    @Test
    void nullMaterial_returnsNull() {
        ItemPrices p = prices(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        WorthQuery.Result r = WorthQuery.compute(null, 0, null, p);
        assertNull(r);
    }
}
