package com.ttsstudio.ftopforge.meta;

import java.util.ArrayList;
import java.util.List;

public final class DynamicBanner {
    private DynamicBanner() {}

    public static String modulesLine(RuntimeState s) {
        List<String> parts = new ArrayList<>();
        if (s.guiEnabled) parts.add("GUI");
        if (s.holoEngineName != null) parts.add("Holo(" + s.holoEngineName + ")");
        if (s.discordEnabled) parts.add("Discord");
        if (s.papiRegistered) parts.add("PAPI");
        if (s.historyEnabled) parts.add("History");
        if (s.forensicsEnabled) parts.add("Forensics");
        if (s.rewardsEnabled) parts.add("Rewards");
        if (s.bstatsEnabled) parts.add("bStats");
        return "modules: " + String.join(" | ", parts);
    }
}
