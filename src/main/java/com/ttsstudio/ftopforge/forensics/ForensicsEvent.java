package com.ttsstudio.ftopforge.forensics;

import java.util.UUID;

/**
 * Immutable value object representing a single forensics row.
 * Fields are intentionally public final — this is a wire-level VO used by the
 * batch writer and DAO, not a behavioural type.
 */
public final class ForensicsEvent {

    public enum Action { PLACE, BREAK }

    public final long ts;
    public final UUID playerUuid;
    public final String playerName;
    public final Action action;
    public final String material;
    public final String world;
    public final int x;
    public final int y;
    public final int z;
    public final int chunkX;
    public final int chunkZ;
    /** Nullable: null means the chunk had no faction owner at the time the event was recorded. */
    public final String territoryFactionId;
    public final boolean isMember;

    public ForensicsEvent(long ts, UUID playerUuid, String playerName, Action action, String material,
                          String world, int x, int y, int z, int chunkX, int chunkZ,
                          String territoryFactionId, boolean isMember) {
        this.ts = ts;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.action = action;
        this.material = material;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.territoryFactionId = territoryFactionId;
        this.isMember = isMember;
    }
}
