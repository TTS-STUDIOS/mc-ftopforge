package com.ttsstudio.ftopforge.core;

public final class AvgBreakdown {
    public final long chunks;
    public final long spawners;
    public final long items;
    public final long blocks;
    public final long balance;

    public AvgBreakdown(long chunks, long spawners, long items, long blocks, long balance) {
        this.chunks = chunks;
        this.spawners = spawners;
        this.items = items;
        this.blocks = blocks;
        this.balance = balance;
    }

    public static AvgBreakdown empty() {
        return new AvgBreakdown(0, 0, 0, 0, 0);
    }
}
