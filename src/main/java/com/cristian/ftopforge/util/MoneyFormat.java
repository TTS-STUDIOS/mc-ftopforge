package com.cristian.ftopforge.util;

public final class MoneyFormat {
    private MoneyFormat() {}

    public static String shortFmt(long v) {
        if (v == 0) return "0";
        boolean neg = v < 0;
        long abs = Math.abs(v);
        String suffix;
        long divisor;
        if (abs >= 1_000_000_000_000L) { suffix = "T"; divisor = 1_000_000_000_000L; }
        else if (abs >= 1_000_000_000L) { suffix = "B"; divisor = 1_000_000_000L; }
        else if (abs >= 1_000_000L) { suffix = "M"; divisor = 1_000_000L; }
        else if (abs >= 1_000L) { suffix = "K"; divisor = 1_000L; }
        else return (neg ? "-" : "") + abs;
        // 1 decimal, half-up rounding
        long scaled = Math.round((double) abs / (divisor / 10.0));
        // 999,950 rounds to 1000.0K: move up a unit instead.
        if (scaled >= 10_000 && !suffix.equals("T")) return shortFmt(neg ? -(divisor * 1000) : divisor * 1000);
        long whole = scaled / 10;
        long frac = scaled % 10;
        return (neg ? "-" : "") + whole + "." + frac + suffix;
    }

    public static String fullFmt(long v) {
        return String.format(java.util.Locale.ROOT, "%,d", v);
    }
}
