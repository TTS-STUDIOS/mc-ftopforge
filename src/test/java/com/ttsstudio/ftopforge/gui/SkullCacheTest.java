package com.ttsstudio.ftopforge.gui;

import com.ttsstudio.ftopforge.storage.SkullCacheDao;
import com.ttsstudio.ftopforge.storage.SkullCacheRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkullCacheTest {

    private SkullCacheDao mockDao;
    private SkullCache cache;
    private final long NOW = 1_700_000_000_000L;
    private final long TTL = 24 * 3600 * 1000L; // 24h

    @BeforeEach
    void setUp() throws SQLException {
        mockDao = mock(SkullCacheDao.class);
        when(mockDao.loadWarmup(anyInt())).thenReturn(Collections.emptyList());
        cache = new SkullCache.Testable(mockDao, TTL, 0, () -> NOW);
    }

    @Test
    void warmup_loadsRows_intoMemory() throws SQLException {
        UUID u = UUID.randomUUID();
        when(mockDao.loadWarmup(200)).thenReturn(
                Collections.singletonList(new SkullCacheRow(u, "tex-value", NOW - 1000)));
        SkullCache.Testable c = new SkullCache.Testable(mockDao, TTL, 200, () -> NOW);
        c.warmupSync();
        assertTrue(c.hasFreshInMemory(u, NOW));
    }

    @Test
    void hasFreshInMemory_returnsFalse_whenAbsent() {
        assertFalse(cache.hasFreshInMemory(UUID.randomUUID(), NOW));
    }

    @Test
    void hasFreshInMemory_returnsFalse_whenStale() throws SQLException {
        UUID u = UUID.randomUUID();
        when(mockDao.loadWarmup(200)).thenReturn(
                Collections.singletonList(new SkullCacheRow(u, "tex", NOW - TTL - 1)));
        SkullCache.Testable c = new SkullCache.Testable(mockDao, TTL, 200, () -> NOW);
        c.warmupSync();
        assertFalse(c.hasFreshInMemory(u, NOW));
    }

    @Test
    void purgeExpired_callsDaoWithCutoff() throws SQLException {
        when(mockDao.deleteExpired(anyLong())).thenReturn(7);
        int deleted = cache.purgeExpired();
        assertEquals(7, deleted);
        verify(mockDao).deleteExpired(NOW - TTL);
    }

    @Test
    void extractTexturesValue_parsesMojangResponse() {
        String json = "{\"id\":\"abc\",\"name\":\"Notch\",\"properties\":[{\"name\":\"textures\",\"value\":\"BASE64STRING\",\"signature\":\"sig\"}]}";
        assertEquals("BASE64STRING", SkullCache.extractTexturesValue(json));
    }

    @Test
    void extractTexturesValue_returnsNull_whenNoProperties() {
        assertNull(SkullCache.extractTexturesValue("{\"id\":\"abc\",\"name\":\"x\"}"));
    }
}
