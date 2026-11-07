package com.ttsstudio.ftopforge.core;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class WorldFilterTest {

    @Test
    void matchesIgnoringCaseAndSpaces() {
        WorldFilter f = new WorldFilter(Arrays.asList("World_Nether", " creative ", "", null));
        assertTrue(f.ignores("world_nether"));
        assertTrue(f.ignores("CREATIVE"));
        assertFalse(f.ignores("world"));
        assertFalse(f.ignores(""));
        assertFalse(f.ignores(null));
    }

    @Test
    void emptyListIgnoresNothing() {
        assertFalse(new WorldFilter(Collections.emptyList()).ignores("world"));
        assertFalse(new WorldFilter(null).ignores("world"));
        assertFalse(WorldFilter.NONE.ignores("world"));
    }
}
