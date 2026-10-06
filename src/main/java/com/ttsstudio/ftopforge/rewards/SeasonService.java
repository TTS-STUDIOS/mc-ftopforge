package com.ttsstudio.ftopforge.rewards;

import com.ttsstudio.ftopforge.meta.MetaDao;

import java.sql.SQLException;
import java.util.Optional;

public class SeasonService {
    private static final String KEY_LAST_PAYOUT = "last_payout_at";
    private static final String KEY_SEASON_STARTED = "season.current_started_at";
    private static final long WEEK_MS = 7L * 86_400_000L;

    private final MetaDao meta;
    private final SeasonsDao seasonsDao;

    public SeasonService(MetaDao meta, SeasonsDao seasonsDao) {
        this.meta = meta;
        this.seasonsDao = seasonsDao;
    }

    public boolean shouldCatchUp(long now) throws SQLException {
        Optional<Long> last = meta.getLong(KEY_LAST_PAYOUT);
        if (!last.isPresent() || last.get() == 0L) return false;
        return now - last.get() >= WEEK_MS;
    }

    public long currentSeasonStartedAt(long fallbackNow) throws SQLException {
        return meta.getLong(KEY_SEASON_STARTED).orElse(fallbackNow);
    }

    public long recordPayout(long now, String top10Json, String payoutsLog) throws SQLException {
        long started = currentSeasonStartedAt(now);
        long id = seasonsDao.insertSeason(new SeasonRow(0L, started, now, top10Json, payoutsLog));
        meta.putLong(KEY_LAST_PAYOUT, now);
        meta.putLong(KEY_SEASON_STARTED, now);
        return id;
    }
}
