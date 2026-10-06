package com.ttsstudio.ftopforge.rewards;

import java.util.Calendar;
import java.util.TimeZone;

public final class CronScheduler {
    private CronScheduler() {}

    public static TimeZone resolveZone(String configValue) {
        if (configValue == null || "system".equalsIgnoreCase(configValue)) return TimeZone.getDefault();
        return TimeZone.getTimeZone(configValue);
    }

    /** dayOfWeek = Calendar.MONDAY..SUNDAY (1=Sunday..7=Saturday in Calendar API). */
    public static long nextRunAt(int dayOfWeek, int hour, int minute, TimeZone tz, long fromMs) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(fromMs);
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        while (c.getTimeInMillis() <= fromMs) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }

    public static int parseDayOfWeek(String name) {
        String n = name == null ? "" : name.toUpperCase();
        switch (n) {
            case "MONDAY": return Calendar.MONDAY;
            case "TUESDAY": return Calendar.TUESDAY;
            case "WEDNESDAY": return Calendar.WEDNESDAY;
            case "THURSDAY": return Calendar.THURSDAY;
            case "FRIDAY": return Calendar.FRIDAY;
            case "SATURDAY": return Calendar.SATURDAY;
            case "SUNDAY": return Calendar.SUNDAY;
            default: throw new IllegalArgumentException("invalid day-of-week: " + name);
        }
    }
}
