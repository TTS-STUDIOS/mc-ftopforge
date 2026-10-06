package com.ttsstudio.ftopforge.holograms;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HologramServiceAccessorsTest {

    @Test
    void currentLocation_returnsClone_notSameReference() {
        World w = Mockito.mock(World.class);
        Mockito.when(w.getName()).thenReturn("world");
        Location loc = new Location(w, 1.5, 80.5, 2.5);
        HologramEngine engine = Mockito.mock(HologramEngine.class);
        HologramService svc = new HologramService(
            Mockito.mock(org.bukkit.plugin.Plugin.class),
            engine, loc, Collections.singletonList("test"), 60,
            Collections::emptyList,
            () -> -1L);

        Location got = svc.currentLocation();

        assertNotNull(got);
        assertNotSame(loc, got, "should be a clone");
        assertEquals(loc.getX(), got.getX(), 0.0);
        assertEquals(loc.getY(), got.getY(), 0.0);
        assertEquals(loc.getZ(), got.getZ(), 0.0);
    }

    @Test
    void isRunning_falseBeforeStart_falseAfterStop_noNpeOnForceRefreshWhenStopped() {
        HologramEngine engine = Mockito.mock(HologramEngine.class);
        HologramService svc = new HologramService(
            Mockito.mock(org.bukkit.plugin.Plugin.class),
            engine, null, Collections.emptyList(), 60,
            Collections::emptyList,
            () -> -1L);

        assertFalse(svc.isRunning(), "fresh service should not be running");

        // forceRefresh on a non-running service must be a no-op (no engine.update call, no NPE).
        svc.forceRefresh();
        Mockito.verifyNoInteractions(engine);
    }
}
