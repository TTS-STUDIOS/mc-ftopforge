package com.ttsstudio.ftopforge.rewards;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CronSchedulerTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long utcMillis(int year, int month1Based, int day, int hour, int minute, int millis) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(year, month1Based - 1, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTimeInMillis();
    }

    @Test
    void nextMondayFromThursday_returnsUpcomingMonday() {
        // Thursday 2026-05-21 14:30 UTC
        long from = utcMillis(2026, 5, 21, 14, 30, 0);
        long expected = utcMillis(2026, 5, 25, 0, 0, 0);
        assertEquals(expected, CronScheduler.nextRunAt(Calendar.MONDAY, 0, 0, UTC, from));
    }

    @Test
    void nextMondayFromMondayPlus1ms_returnsNextWeek() {
        // Monday 2026-05-25 00:00:00.001 UTC
        long from = utcMillis(2026, 5, 25, 0, 0, 1);
        long expected = utcMillis(2026, 6, 1, 0, 0, 0);
        assertEquals(expected, CronScheduler.nextRunAt(Calendar.MONDAY, 0, 0, UTC, from));
    }

    @Test
    void weeklyCycle_addsExactly7DaysBetweenCalls() {
        // Start: Sunday 2026-05-24 12:00 UTC (so next MONDAY 00:00 is 2026-05-25)
        long t0 = utcMillis(2026, 5, 24, 12, 0, 0);
        long t1 = CronScheduler.nextRunAt(Calendar.MONDAY, 0, 0, UTC, t0);
        // From t1+1ms, the next Monday should be t1 + 7 days
        long t2 = CronScheduler.nextRunAt(Calendar.MONDAY, 0, 0, UTC, t1 + 1);
        assertEquals(t1 + 7L * 86_400_000L, t2);
    }

    @Test
    void resolveZone_systemReturnsDefault() {
        assertEquals(TimeZone.getDefault(), CronScheduler.resolveZone("system"));
        assertEquals(TimeZone.getDefault(), CronScheduler.resolveZone(null));
        assertEquals(TimeZone.getDefault(), CronScheduler.resolveZone("SYSTEM"));
    }

    @Test
    void parseDayOfWeek_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> CronScheduler.parseDayOfWeek("FUNDAY"));
        assertEquals(Calendar.MONDAY, CronScheduler.parseDayOfWeek("monday"));
        assertEquals(Calendar.FRIDAY, CronScheduler.parseDayOfWeek("FRIDAY"));
    }
}
