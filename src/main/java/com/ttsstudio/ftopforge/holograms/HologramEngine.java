package com.ttsstudio.ftopforge.holograms;

import org.bukkit.Location;

import java.util.List;

public interface HologramEngine {
    String name();
    void create(Location loc, List<String> lines);
    void update(List<String> lines);
    void remove();
}
