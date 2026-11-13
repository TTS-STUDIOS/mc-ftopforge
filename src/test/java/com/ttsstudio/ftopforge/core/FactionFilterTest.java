package com.ttsstudio.ftopforge.core;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class FactionFilterTest {

    @Test
    void matchesTagsIgnoringCaseAndSpaces() {
        FactionFilter f = new FactionFilter(Arrays.asList("Staff", " events ", "", null));
        assertTrue(f.ignores("staff"));
        assertTrue(f.ignores("EVENTS"));
        assertTrue(f.ignores(" Staff "));
        assertFalse(f.ignores("Stafford"));
        assertFalse(f.ignores(""));
        assertFalse(f.ignores(null));
    }

    @Test
    void emptyListIgnoresNothing() {
        assertFalse(new FactionFilter(Collections.emptyList()).ignores("Staff"));
        assertFalse(new FactionFilter(null).ignores("Staff"));
        assertFalse(FactionFilter.NONE.ignores("Staff"));
    }
}
