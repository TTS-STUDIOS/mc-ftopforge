package com.ttsstudio.ftopforge.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterialKeyTest {

    @Test
    void parse_nameOnly_dataZero() {
        MaterialKey k = MaterialKey.parse("DIAMOND");
        assertEquals("DIAMOND", k.name);
        assertEquals((short) 0, k.data);
    }

    @Test
    void parse_nameAndData_extracted() {
        MaterialKey k = MaterialKey.parse("STAINED_GLASS_PANE-7");
        assertEquals("STAINED_GLASS_PANE", k.name);
        assertEquals((short) 7, k.data);
    }

    @Test
    void parse_invalidData_dataZero() {
        MaterialKey k = MaterialKey.parse("DIAMOND-notanumber");
        assertEquals("DIAMOND", k.name);
        assertEquals((short) 0, k.data);
    }

    @Test
    void parse_emptyOrNull_returnsAir() {
        assertEquals("AIR", MaterialKey.parse(null).name);
        assertEquals("AIR", MaterialKey.parse("").name);
    }
}
