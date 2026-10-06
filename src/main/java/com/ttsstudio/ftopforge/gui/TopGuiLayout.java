package com.ttsstudio.ftopforge.gui;

public final class TopGuiLayout {

    public static final int PER_PAGE = 45;
    public static final int INV_SIZE = 54;

    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;

    private TopGuiLayout() {}

    public static int totalPages(int factionCount) {
        if (factionCount <= 0) return 1;
        return (factionCount + PER_PAGE - 1) / PER_PAGE;
    }

    public static int clampPage(int requested, int totalPages) {
        if (requested < 1) return 1;
        if (requested > totalPages) return totalPages;
        return requested;
    }

    /** Returns [fromInclusive, toExclusive] over the full sorted list. */
    public static int[] rangeForPage(int page, int factionCount) {
        int from = (page - 1) * PER_PAGE;
        int to = Math.min(from + PER_PAGE, factionCount);
        return new int[]{from, to};
    }

    public static boolean isPrevVisible(int page) {
        return page > 1;
    }

    public static boolean isNextVisible(int page, int totalPages) {
        return page < totalPages;
    }
}
