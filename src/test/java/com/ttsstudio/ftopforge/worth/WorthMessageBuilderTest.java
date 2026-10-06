package com.ttsstudio.ftopforge.worth;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorthMessageBuilderTest {

    @Test
    void buildFullMessage_includesMaterialAmountUnitTotal() {
        WorthQuery.Result r = new WorthQuery.Result("DIAMOND_BLOCK", 500L, 32_000L, 64, false);
        List<String> lines = WorthMessageBuilder.build(r);
        assertEquals(3, lines.size());  // no notListed line
        assertTrue(lines.get(0).contains("DIAMOND_BLOCK"));
        assertTrue(lines.get(0).contains("64"));
        assertTrue(lines.get(1).contains("500"));
        assertTrue(lines.get(2).contains("32,000"));
    }
}
