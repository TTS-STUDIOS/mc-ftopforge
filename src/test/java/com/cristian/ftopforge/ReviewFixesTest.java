package com.cristian.ftopforge;

import com.cristian.ftopforge.core.FactionSnapshot;
import com.cristian.ftopforge.forensics.ForensicsDao;
import com.cristian.ftopforge.forensics.ForensicsEvent;
import com.cristian.ftopforge.meta.MetaDao;
import com.cristian.ftopforge.rewards.PayoutRunner;
import com.cristian.ftopforge.rewards.SeasonService;
import com.cristian.ftopforge.rewards.SeasonsDao;
import com.cristian.ftopforge.storage.Dialect;
import com.cristian.ftopforge.storage.JdbcRepository;
import com.cristian.ftopforge.storage.SnapshotDao;
import com.cristian.ftopforge.util.MoneyFormat;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One test per defect of the 5 October 2026 code review. */
class ReviewFixesTest {

    private HikariDataSource ds;
    private JdbcRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        repo = new JdbcRepository(ds, Dialect.SQLITE);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(Dialect.SQLITE.recalcsDdl());
            s.executeUpdate(Dialect.SQLITE.snapshotsCurrentDdl());
            s.executeUpdate(Dialect.SQLITE.forensicsDdl());
            s.executeUpdate(Dialect.SQLITE.seasonsDdl());
            s.executeUpdate(Dialect.SQLITE.metaDdl());
        }
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    @Test
    void theRankingSurvivesARestart() throws Exception {
        SnapshotDao dao = new SnapshotDao(repo);
        dao.upsert(FactionSnapshot.builder("gone", "Gone").addBalanceValue(999).build(), 1L);
        dao.upsert(FactionSnapshot.builder("f1", "Reds").leader("u1", "Leader")
            .addSpawnersValue(500).addBalanceValue(100).incSpawnerCount("BLAZE").incSpawnerCount("BLAZE")
            .build(), 2L);

        List<FactionSnapshot> back = dao.loadLatest();

        assertEquals(1, back.size(), "a faction missing from the newest recalculation is not brought back");
        FactionSnapshot reds = back.get(0);
        assertEquals("Reds", reds.factionName());
        assertEquals(600, reds.totalValue());
        assertEquals("Leader", reds.leaderName());
        assertEquals(Integer.valueOf(2), reds.spawnersCount().get("BLAZE"));
    }

    @Test
    void anEmptyRankingPaysNobodyAndIsNotRecorded() throws Exception {
        MetaDao meta = new MetaDao(ds);
        SeasonService seasons = new SeasonService(meta, new SeasonsDao(ds));
        List<String> dispatched = new ArrayList<>();
        PayoutRunner runner = new PayoutRunner(seasons,
            Collections.singletonMap(1, Collections.singletonList("eco give %faction_leader_name% 100")),
            false, null, dispatched::add, msg -> { }, null, null, Logger.getAnonymousLogger());

        assertFalse(runner.run(new ArrayList<FactionSnapshot>(), 5_000L));

        assertTrue(dispatched.isEmpty());
        assertFalse(meta.getLong("last_payout_at").isPresent(), "the week must not count as paid");
    }

    @Test
    void theInsiderFilterShowsMembers() throws Exception {
        ForensicsDao dao = new ForensicsDao(ds);
        UUID member = UUID.randomUUID();
        UUID raider = UUID.randomUUID();
        dao.insertBatch(Arrays.asList(
            new ForensicsEvent(1L, member, "Member", ForensicsEvent.Action.BREAK, "MOB_SPAWNER",
                "world", 0, 64, 0, 0, 0, "facA", true),
            new ForensicsEvent(2L, raider, "Raider", ForensicsEvent.Action.BREAK, "MOB_SPAWNER",
                "world", 0, 64, 0, 0, 0, "facA", false)));

        List<ForensicsEvent> insiders = dao.queryByFaction("facA", null, true, 0, 10);

        assertEquals(1, insiders.size());
        assertEquals("Member", insiders.get(0).playerName);
    }

    @Test
    void aValueThatRoundsUpToTheNextUnitUsesIt() {
        assertEquals("1.0M", MoneyFormat.shortFmt(999_950));
        assertEquals("1.0B", MoneyFormat.shortFmt(999_999_999));
        assertEquals("999.9K", MoneyFormat.shortFmt(999_900));
        assertEquals("-1.0M", MoneyFormat.shortFmt(-999_950));
    }
}
