package com.ttsstudio.ftopforge.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopGuiLayoutTest {

    @Test
    void totalPages_zeroFactions_isOne() {
        assertEquals(1, TopGuiLayout.totalPages(0));
    }

    @Test
    void totalPages_oneToFortyFive_isOne() {
        assertEquals(1, TopGuiLayout.totalPages(1));
        assertEquals(1, TopGuiLayout.totalPages(45));
    }

    @Test
    void totalPages_fortySix_isTwo() {
        assertEquals(2, TopGuiLayout.totalPages(46));
        assertEquals(2, TopGuiLayout.totalPages(90));
    }

    @Test
    void totalPages_ninetyOne_isThree() {
        assertEquals(3, TopGuiLayout.totalPages(91));
    }

    @Test
    void clampPage_belowOne_isOne() {
        assertEquals(1, TopGuiLayout.clampPage(0, 5));
        assertEquals(1, TopGuiLayout.clampPage(-3, 5));
    }

    @Test
    void clampPage_aboveTotal_isTotal() {
        assertEquals(5, TopGuiLayout.clampPage(99, 5));
    }

    @Test
    void rangeForPage_pageOne_returnsZeroToFortyFive() {
        int[] r = TopGuiLayout.rangeForPage(1, 100);
        assertEquals(0, r[0]);
        assertEquals(45, r[1]);
    }

    @Test
    void rangeForPage_pageTwo_with46Factions_returnsFortyFiveToFortySix() {
        int[] r = TopGuiLayout.rangeForPage(2, 46);
        assertEquals(45, r[0]);
        assertEquals(46, r[1]);
    }

    @Test
    void isPrevVisible_falseOnPage1() {
        assertFalse(TopGuiLayout.isPrevVisible(1));
        assertTrue(TopGuiLayout.isPrevVisible(2));
    }

    @Test
    void isNextVisible_trueWhenMorePagesExist() {
        assertTrue(TopGuiLayout.isNextVisible(1, 2));
        assertFalse(TopGuiLayout.isNextVisible(2, 2));
    }
}
