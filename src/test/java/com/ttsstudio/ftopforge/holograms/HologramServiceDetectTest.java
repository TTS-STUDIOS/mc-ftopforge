package com.ttsstudio.ftopforge.holograms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class HologramServiceDetectTest {

    private static Logger quietLogger(List<String> warnings) {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) {
                if (r.getLevel() == Level.WARNING) warnings.add(r.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    @Test
    void auto_withDhPresent_picksDh() {
        HologramEngine e = HologramService.detect("auto",
            name -> name.equals("DecentHolograms"), null, quietLogger(new ArrayList<>()));
        assertNotNull(e);
        assertEquals("DecentHolograms", e.name());
    }

    @Test
    void auto_withNeither_returnsNullAndWarns() {
        List<String> warnings = new ArrayList<>();
        HologramEngine e = HologramService.detect("auto",
            name -> false, null, quietLogger(warnings));
        assertNull(e);
        assertFalse(warnings.isEmpty());
    }

    @Test
    void engineHolographicDisplays_returnsNullSinceHDDropped() {
        List<String> warnings = new ArrayList<>();
        HologramEngine e = HologramService.detect("holographicdisplays",
            name -> true, null, quietLogger(warnings));
        assertNull(e);
        assertTrue(warnings.stream().anyMatch(s -> s.contains("no soportado")));
    }

    @Test
    void engineDecentholograms_withoutDh_returnsNull() {
        List<String> warnings = new ArrayList<>();
        HologramEngine e = HologramService.detect("decentholograms",
            name -> false, null, quietLogger(warnings));
        assertNull(e);
        assertFalse(warnings.isEmpty());
    }
}
