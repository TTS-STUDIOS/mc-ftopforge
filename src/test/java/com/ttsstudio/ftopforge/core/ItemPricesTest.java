package com.ttsstudio.ftopforge.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ItemPricesTest {

    @Test
    void spawnerLookupReturnsZeroForUnknown() {
        ItemPrices p = new ItemPrices(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, 0);
        assertEquals(0L, p.spawnerValue("CREEPER"));
    }

    @Test
    void spawnerLookupIsCaseInsensitive() {
        Map<String, Long> spawners = new HashMap<>();
        spawners.put("CREEPER", 500_000L);
        ItemPrices p = new ItemPrices(new HashMap<>(), spawners, new HashMap<>(), 0, 0);
        assertEquals(500_000L, p.spawnerValue("creeper"));
        assertEquals(500_000L, p.spawnerValue("CREEPER"));
        assertEquals(500_000L, p.spawnerValue("Creeper"));
    }

    @Test
    void blockLookupReturnsZeroForUnknown() {
        ItemPrices p = new ItemPrices(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, 0);
        assertEquals(0L, p.blockValue("BEACON"));
    }

    @Test
    void itemLookupByCompoundKey() {
        Map<String, Long> items = new HashMap<>();
        items.put("DIAMOND_SWORD", 500L);
        items.put("GOLDEN_APPLE", 20L);
        ItemPrices p = new ItemPrices(items, new HashMap<>(), new HashMap<>(), 0, 0);
        assertEquals(500L, p.itemValue("DIAMOND_SWORD"));
        assertEquals(20L, p.itemValue("golden_apple"));
        assertEquals(0L, p.itemValue("STICK"));
    }

    @Test
    void godAppleScalarIsExposed() {
        ItemPrices p = new ItemPrices(new HashMap<>(), new HashMap<>(), new HashMap<>(), 5000L, 0L);
        assertEquals(5000L, p.godAppleValue());
    }
}
