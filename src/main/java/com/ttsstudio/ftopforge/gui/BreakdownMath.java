package com.ttsstudio.ftopforge.gui;

public final class BreakdownMath {

    private BreakdownMath() {}

    public static int pctOf(long component, long total) {
        if (total <= 0) return 0;
        long p = (component * 100) / total;
        if (p > 100) return 100;
        if (p < 0) return 0;
        return (int) p;
    }

    public static Delta delta(long value, long avg) {
        if (avg <= 0) return new Delta(0, '=', "&7");
        long diff = value - avg;
        // Use double intermediate to avoid overflow when diff is very large.
        int absPct = (int) Math.min(((double) Math.abs(diff) / avg) * 100, Integer.MAX_VALUE);
        if (diff > 0) return new Delta(absPct, '+', "&a");
        if (diff < 0) return new Delta(absPct, '-', "&c");
        return new Delta(0, '=', "&7");
    }

    public static final class Delta {
        public final int pct;
        public final char sign;
        public final String color;
        public Delta(int pct, char sign, String color) {
            this.pct = pct;
            this.sign = sign;
            this.color = color;
        }
    }
}
