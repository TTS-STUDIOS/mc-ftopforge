package com.ttsstudio.ftopforge.rewards;

public final class SeasonRow {
    private final long seasonId;
    private final long startedAt;
    private final long endedAt;
    private final String top10Json;
    private final String payoutsLog;

    public SeasonRow(long seasonId, long startedAt, long endedAt, String top10Json, String payoutsLog) {
        this.seasonId = seasonId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.top10Json = top10Json;
        this.payoutsLog = payoutsLog;
    }

    public long seasonId() { return seasonId; }
    public long startedAt() { return startedAt; }
    public long endedAt() { return endedAt; }
    public String top10Json() { return top10Json; }
    public String payoutsLog() { return payoutsLog; }
}
