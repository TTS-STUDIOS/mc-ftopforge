package com.ttsstudio.ftopforge.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BreakdownMathTest {

    @Test
    void pctOf_totalZero_returnsZero() {
        assertEquals(0, BreakdownMath.pctOf(100, 0));
    }

    @Test
    void pctOf_normal_truncatesToInt() {
        assertEquals(25, BreakdownMath.pctOf(250, 1000));
        assertEquals(33, BreakdownMath.pctOf(333, 1000));
    }

    @Test
    void pctOf_componentLargerThanTotal_caps100() {
        assertEquals(100, BreakdownMath.pctOf(2000, 1000));
    }

    @Test
    void pctOf_negativeComponent_returnsZero() {
        assertEquals(0, BreakdownMath.pctOf(-500, 1000));
    }

    @Test
    void delta_avgZero_returnsZeroNeutral() {
        BreakdownMath.Delta d = BreakdownMath.delta(500, 0);
        assertEquals(0, d.pct);
        assertEquals('=', d.sign);
        assertEquals("&7", d.color);
    }

    @Test
    void delta_above_returnsPositiveGreen() {
        BreakdownMath.Delta d = BreakdownMath.delta(150, 100);
        assertEquals(50, d.pct);
        assertEquals('+', d.sign);
        assertEquals("&a", d.color);
    }

    @Test
    void delta_below_returnsNegativeRed() {
        BreakdownMath.Delta d = BreakdownMath.delta(75, 100);
        assertEquals(25, d.pct);
        assertEquals('-', d.sign);
        assertEquals("&c", d.color);
    }

    @Test
    void delta_equal_returnsZeroGray() {
        BreakdownMath.Delta d = BreakdownMath.delta(100, 100);
        assertEquals(0, d.pct);
        assertEquals('=', d.sign);
        assertEquals("&7", d.color);
    }
}
