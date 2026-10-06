package com.ttsstudio.ftopforge.core;

import java.util.HashMap;
import java.util.Map;

public final class FactionSnapshot {

    private final String factionId;
    private final String factionName;
    private final String leaderUuid;
    private final String leaderName;
    private final long totalValue;
    private final long chunksValue;
    private final long spawnersValue;
    private final long itemsValue;
    private final long blocksValue;
    private final long balanceValue;
    private final int chunksCount;
    private final Map<String, Integer> spawnersCount;
    private final Map<String, Integer> blocksCount;
    private final String richestMemberUuid;
    private final long richestMemberBalance;
    private final long calculatedAt;

    private FactionSnapshot(Builder b) {
        this.factionId = b.factionId;
        this.factionName = b.factionName;
        this.leaderUuid = b.leaderUuid;
        this.leaderName = b.leaderName;
        this.chunksValue = b.chunksValue;
        this.spawnersValue = b.spawnersValue;
        this.itemsValue = b.itemsValue;
        this.blocksValue = b.blocksValue;
        this.balanceValue = b.balanceValue;
        this.totalValue = b.chunksValue + b.spawnersValue + b.itemsValue + b.blocksValue + b.balanceValue;
        this.chunksCount = b.chunksCount;
        this.spawnersCount = b.spawnersCount;
        this.blocksCount = b.blocksCount;
        this.richestMemberUuid = b.richestMemberUuid;
        this.richestMemberBalance = b.richestMemberBalance;
        this.calculatedAt = b.calculatedAt;
    }

    // getters
    public String factionId() { return factionId; }
    public String factionName() { return factionName; }
    public String leaderUuid() { return leaderUuid; }
    public String leaderName() { return leaderName; }
    public long totalValue() { return totalValue; }
    public long chunksValue() { return chunksValue; }
    public long spawnersValue() { return spawnersValue; }
    public long itemsValue() { return itemsValue; }
    public long blocksValue() { return blocksValue; }
    public long balanceValue() { return balanceValue; }
    public int chunksCount() { return chunksCount; }
    public Map<String, Integer> spawnersCount() { return spawnersCount; }
    public Map<String, Integer> blocksCount() { return blocksCount; }
    public String richestMemberUuid() { return richestMemberUuid; }
    public long richestMemberBalance() { return richestMemberBalance; }
    public long calculatedAt() { return calculatedAt; }

    public static Builder builder(String factionId, String factionName) {
        return new Builder(factionId, factionName);
    }

    public static final class Builder {
        private final String factionId;
        private final String factionName;
        private String leaderUuid;
        private String leaderName;
        private long chunksValue, spawnersValue, itemsValue, blocksValue, balanceValue;
        private int chunksCount;
        private final Map<String, Integer> spawnersCount = new HashMap<>();
        private final Map<String, Integer> blocksCount = new HashMap<>();
        private String richestMemberUuid;
        private long richestMemberBalance;
        private long calculatedAt = System.currentTimeMillis();

        Builder(String factionId, String factionName) {
            this.factionId = factionId;
            this.factionName = factionName;
        }

        public Builder leader(String uuid, String name) { this.leaderUuid = uuid; this.leaderName = name; return this; }
        public Builder addChunksValue(long v) { this.chunksValue += v; return this; }
        public Builder addSpawnersValue(long v) { this.spawnersValue += v; return this; }
        public Builder addItemsValue(long v) { this.itemsValue += v; return this; }
        public Builder addBlocksValue(long v) { this.blocksValue += v; return this; }
        public Builder addBalanceValue(long v) { this.balanceValue += v; return this; }
        public Builder incChunkCount() { this.chunksCount++; return this; }
        public Builder chunksCount(int n) { this.chunksCount = n; return this; }
        public Builder calculatedAt(long at) { this.calculatedAt = at; return this; }
        public Builder spawnerCount(String mob, int n) { spawnersCount.put(mob.toUpperCase(), n); return this; }
        public Builder blockCount(String material, int n) { blocksCount.put(material.toUpperCase(), n); return this; }
        public Builder richest(String uuid, long balance) {
            this.richestMemberUuid = uuid; this.richestMemberBalance = balance; return this;
        }
        public Builder incSpawnerCount(String mob) {
            spawnersCount.merge(mob.toUpperCase(), 1, Integer::sum);
            return this;
        }
        public Builder incBlockCount(String material) {
            blocksCount.merge(material.toUpperCase(), 1, Integer::sum);
            return this;
        }
        public Builder maybeRichest(String uuid, long balance) {
            if (balance > richestMemberBalance) {
                this.richestMemberUuid = uuid;
                this.richestMemberBalance = balance;
            }
            return this;
        }
        public FactionSnapshot build() { return new FactionSnapshot(this); }
    }
}
