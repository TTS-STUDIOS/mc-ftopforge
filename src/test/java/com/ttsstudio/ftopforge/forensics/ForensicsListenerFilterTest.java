package com.ttsstudio.ftopforge.forensics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ForensicsListenerFilterTest {

    private static Set<String> whitelist() {
        return new HashSet<>(Arrays.asList("BEACON", "HOPPER", "CHEST"));
    }

    @Test
    void whitelistedBlockInClaimedChunk_recordsTrue() {
        ForensicsListenerFilter.Result r = ForensicsListenerFilter.decide(
            "BEACON", "facA", "facA", whitelist(), false);
        assertTrue(r.shouldRecord);
        assertTrue(r.isMember);
    }

    @Test
    void notWhitelistedMaterial_recordsFalse() {
        ForensicsListenerFilter.Result r = ForensicsListenerFilter.decide(
            "DIRT", "facA", "facA", whitelist(), false);
        assertFalse(r.shouldRecord);
    }

    @Test
    void wildernessChunk_recordsFalse() {
        ForensicsListenerFilter.Result r = ForensicsListenerFilter.decide(
            "BEACON", "facA", null, whitelist(), false);
        assertFalse(r.shouldRecord);
    }

    @Test
    void isMemberFlag_trueWhenPlayerFactionMatchesTerritory() {
        ForensicsListenerFilter.Result rMember = ForensicsListenerFilter.decide(
            "HOPPER", "facX", "facX", whitelist(), false);
        assertTrue(rMember.shouldRecord);
        assertTrue(rMember.isMember);

        ForensicsListenerFilter.Result rOutsider = ForensicsListenerFilter.decide(
            "HOPPER", "facY", "facX", whitelist(), false);
        assertTrue(rOutsider.shouldRecord);
        assertFalse(rOutsider.isMember);

        ForensicsListenerFilter.Result rFactionless = ForensicsListenerFilter.decide(
            "HOPPER", null, "facX", whitelist(), false);
        assertTrue(rFactionless.shouldRecord);
        assertFalse(rFactionless.isMember);
    }

    @Test
    void nonMembersOnly_skipsMembersButKeepsOutsiders() {
        ForensicsListenerFilter.Result rMember = ForensicsListenerFilter.decide(
            "CHEST", "facA", "facA", whitelist(), true);
        assertFalse(rMember.shouldRecord);
        assertTrue(rMember.isMember);

        ForensicsListenerFilter.Result rOutsider = ForensicsListenerFilter.decide(
            "CHEST", "facB", "facA", whitelist(), true);
        assertTrue(rOutsider.shouldRecord);
        assertFalse(rOutsider.isMember);
    }
}
