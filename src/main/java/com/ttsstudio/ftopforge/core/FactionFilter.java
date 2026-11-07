package com.ttsstudio.ftopforge.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Factions left out of the top by tag (case-insensitive), e.g. staff or event factions. */
public final class FactionFilter {

    public static final FactionFilter NONE = new FactionFilter(Collections.emptyList());

    private final Set<String> ignored = new HashSet<>();

    public FactionFilter(List<String> tags) {
        if (tags != null) {
            for (String t : tags) {
                if (t != null && !t.trim().isEmpty()) ignored.add(t.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    public boolean ignores(String tag) {
        return tag != null && ignored.contains(tag.trim().toLowerCase(Locale.ROOT));
    }
}
