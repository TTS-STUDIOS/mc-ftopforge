package com.ttsstudio.ftopforge.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;

public final class DhEngine implements HologramEngine {
    private static final String HOLO_NAME = "ftopforge-top";
    private Hologram holo;

    @Override public String name() { return "DecentHolograms"; }

    @Override public void create(Location loc, List<String> lines) {
        Hologram existing = DHAPI.getHologram(HOLO_NAME);
        if (existing != null) DHAPI.removeHologram(HOLO_NAME);
        holo = DHAPI.createHologram(HOLO_NAME, loc, lines);
    }

    @Override public void update(List<String> lines) {
        if (holo == null) return;
        DHAPI.setHologramLines(holo, lines);
    }

    @Override public void remove() {
        if (holo != null) { DHAPI.removeHologram(HOLO_NAME); holo = null; }
    }
}
