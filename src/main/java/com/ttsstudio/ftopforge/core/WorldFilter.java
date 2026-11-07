package com.ttsstudio.ftopforge.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Worlds whose claims are left out of the worth calculation (case-insensitive). */
public final class WorldFilter {

    public static final WorldFilter NONE = new WorldFilter(Collections.emptyList());

    private final Set<String> ignored = new HashSet<>();

    public WorldFilter(List<String> worlds) {
        if (worlds != null) {
            for (String w : worlds) {
                if (w != null && !w.trim().isEmpty()) ignored.add(w.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    public boolean ignores(String world) {
        return world != null && ignored.contains(world.toLowerCase(Locale.ROOT));
    }
}
