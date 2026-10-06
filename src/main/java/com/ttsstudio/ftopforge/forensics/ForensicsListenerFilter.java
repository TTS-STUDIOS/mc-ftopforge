package com.ttsstudio.ftopforge.forensics;

import java.util.Set;

/**
 * Pure-logic predicate for whether a block-event should be recorded as forensics.
 * Extracted from {@code ForensicsListener} so it can be unit-tested without a
 * Bukkit runtime. The listener itself is the thin glue layer that pulls the
 * inputs out of {@code BlockPlaceEvent}/{@code BlockBreakEvent} and calls
 * {@link #decide}.
 */
public final class ForensicsListenerFilter {

    private ForensicsListenerFilter() {}

    public static final class Result {
        public final boolean shouldRecord;
        public final boolean isMember;
        public Result(boolean shouldRecord, boolean isMember) {
            this.shouldRecord = shouldRecord;
            this.isMember = isMember;
        }
    }

    /**
     * Decide whether this event is recordable and compute the {@code is_member} flag.
     *
     * @param material            Bukkit material name (e.g. "BEACON").
     * @param playerFactionId     player's faction id, or null if factionless.
     * @param territoryFactionId  faction id of the chunk being modified, or null for wilderness.
     * @param whitelist           set of material names that qualify for recording.
     * @param nonMembersOnly      when true, skip events whose actor is a member of the chunk's faction.
     */
    public static Result decide(String material, String playerFactionId, String territoryFactionId,
                                Set<String> whitelist, boolean nonMembersOnly) {
        if (territoryFactionId == null) return new Result(false, false);
        if (!whitelist.contains(material)) return new Result(false, false);
        boolean member = playerFactionId != null && playerFactionId.equals(territoryFactionId);
        if (nonMembersOnly && member) return new Result(false, member);
        return new Result(true, member);
    }
}
