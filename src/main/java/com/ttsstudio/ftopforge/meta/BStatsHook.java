package com.ttsstudio.ftopforge.meta;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

public final class BStatsHook {
    private static final int PLUGIN_ID = 31455;  // https://bstats.org/plugin/bukkit/FTopForge/31455

    private Metrics metrics;

    public void enable(JavaPlugin plugin,
                       IntSupplier factionCount,
                       IntSupplier lastRecalcDurationMs,
                       RuntimeState state) {
        metrics = new Metrics(plugin, PLUGIN_ID);
        metrics.addCustomChart(new SingleLineChart("faction_count", factionCount::getAsInt));
        metrics.addCustomChart(new SingleLineChart("recalc_duration_ms", lastRecalcDurationMs::getAsInt));
        metrics.addCustomChart(new DrilldownPie("modules_enabled", () -> enabledModulesMap(state)));
    }

    private Map<String, Map<String, Integer>> enabledModulesMap(RuntimeState s) {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        if (s.guiEnabled) map.put("gui", Collections.singletonMap("enabled", 1));
        if (s.holoEngineName != null) map.put("holograms", Collections.singletonMap(s.holoEngineName, 1));
        if (s.discordEnabled) map.put("discord", Collections.singletonMap("enabled", 1));
        if (s.papiRegistered) map.put("papi", Collections.singletonMap("enabled", 1));
        if (s.historyEnabled) map.put("history", Collections.singletonMap("enabled", 1));
        if (s.forensicsEnabled) map.put("forensics", Collections.singletonMap("enabled", 1));
        if (s.rewardsEnabled) map.put("rewards", Collections.singletonMap("enabled", 1));
        return map;
    }
}
