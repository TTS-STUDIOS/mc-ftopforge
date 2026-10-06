package com.ttsstudio.ftopforge.history;

public final class HistoryPoint {
    private final long recalcId, finishedAt, totalWorth, chunksValue, spawnersValue, itemsValue, blocksValue, balance;
    private final String factionId, factionName;
    private final int rank;

    public HistoryPoint(long recalcId, long finishedAt, String factionId, String factionName,
                        int rank, long totalWorth, long chunksValue, long spawnersValue,
                        long itemsValue, long blocksValue, long balance) {
        this.recalcId = recalcId; this.finishedAt = finishedAt;
        this.factionId = factionId; this.factionName = factionName;
        this.rank = rank; this.totalWorth = totalWorth;
        this.chunksValue = chunksValue; this.spawnersValue = spawnersValue;
        this.itemsValue = itemsValue; this.blocksValue = blocksValue;
        this.balance = balance;
    }
    public long recalcId() { return recalcId; }
    public long finishedAt() { return finishedAt; }
    public String factionId() { return factionId; }
    public String factionName() { return factionName; }
    public int rank() { return rank; }
    public long totalWorth() { return totalWorth; }
    public long chunksValue() { return chunksValue; }
    public long spawnersValue() { return spawnersValue; }
    public long itemsValue() { return itemsValue; }
    public long blocksValue() { return blocksValue; }
    public long balance() { return balance; }
}
