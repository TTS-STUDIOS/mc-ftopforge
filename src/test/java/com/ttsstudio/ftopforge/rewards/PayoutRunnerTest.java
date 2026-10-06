package com.ttsstudio.ftopforge.rewards;

import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.meta.MetaDao;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PayoutRunnerTest {

    /** No-op SeasonService that doesn't touch DB. */
    private static SeasonService stubSeasonService() {
        return new SeasonService((MetaDao) null, (SeasonsDao) null) {
            @Override
            public long recordPayout(long now, String top10Json, String payoutsLog) throws SQLException {
                return 1L;
            }
        };
    }

    @Test
    void dispatchesAllThreeRanks_whenTopHas3Plus() {
        List<String> dispatched = new ArrayList<>();
        Map<Integer, List<String>> ranks = new LinkedHashMap<>();
        ranks.put(1, Arrays.asList("eco give %faction_leader_name% 100"));
        ranks.put(2, Arrays.asList("eco give %faction_leader_name% 50"));
        ranks.put(3, Arrays.asList("eco give %faction_leader_name% 25"));
        PayoutRunner runner = new PayoutRunner(stubSeasonService(), ranks, false, null,
            dispatched::add, msg -> {}, null, null, Logger.getAnonymousLogger());
        List<FactionSnapshot> top = Arrays.asList(
            FactionSnapshot.builder("f1", "F1").leader("u1", "L1").addBalanceValue(100).build(),
            FactionSnapshot.builder("f2", "F2").leader("u2", "L2").addBalanceValue(80).build(),
            FactionSnapshot.builder("f3", "F3").leader("u3", "L3").addBalanceValue(60).build());
        assertTrue(runner.run(top, 1000L));
        assertEquals(3, dispatched.size());
        assertTrue(dispatched.get(0).contains("L1"));
        assertTrue(dispatched.get(2).contains("L3"));
    }

    @Test
    void dispatchesAvailableRanksOnly_whenTopHasFewer() {
        List<String> dispatched = new ArrayList<>();
        Map<Integer, List<String>> ranks = new LinkedHashMap<>();
        ranks.put(1, Arrays.asList("eco give %faction_leader_name% 100"));
        ranks.put(2, Arrays.asList("eco give %faction_leader_name% 50"));
        ranks.put(3, Arrays.asList("eco give %faction_leader_name% 25"));
        PayoutRunner runner = new PayoutRunner(stubSeasonService(), ranks, false, null,
            dispatched::add, msg -> {}, null, null, Logger.getAnonymousLogger());
        List<FactionSnapshot> top = Arrays.asList(
            FactionSnapshot.builder("f1", "F1").leader("u1", "L1").addBalanceValue(100).build());
        assertTrue(runner.run(top, 1000L));
        assertEquals(1, dispatched.size());
    }

    @Test
    void freezeFlagSetThenReleased() {
        AtomicInteger frozen = new AtomicInteger(0);
        AtomicInteger unfrozen = new AtomicInteger(0);
        PayoutRunner runner = new PayoutRunner(stubSeasonService(),
            Collections.singletonMap(1, Collections.singletonList("noop")),
            false, null, cmd -> {}, msg -> {},
            frozen::incrementAndGet, unfrozen::incrementAndGet,
            Logger.getAnonymousLogger());
        runner.run(Arrays.asList(FactionSnapshot.builder("f1", "F1").build()), 1000L);
        assertEquals(1, frozen.get());
        assertEquals(1, unfrozen.get());
        assertFalse(runner.isInProgress());
    }
}
