package com.ttsstudio.ftopforge.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;

public class SpawnerValuator {

    private final ItemPrices prices;

    public SpawnerValuator(ItemPrices prices) {
        this.prices = prices;
    }

    public long valueOf(Block b) {
        if (b == null || b.getType() != Material.MOB_SPAWNER) return 0L;
        try {
            CreatureSpawner cs = (CreatureSpawner) b.getState();
            if (cs.getSpawnedType() == null) return 0L;
            return prices.spawnerValue(cs.getSpawnedType().name());
        } catch (ClassCastException | NullPointerException e) {
            return 0L;
        }
    }

    /** For tracking counts in the snapshot. Returns the mob name or null if not a spawner. */
    public String mobOf(Block b) {
        if (b == null || b.getType() != Material.MOB_SPAWNER) return null;
        try {
            CreatureSpawner cs = (CreatureSpawner) b.getState();
            return cs.getSpawnedType() == null ? null : cs.getSpawnedType().name();
        } catch (Exception e) {
            return null;
        }
    }
}
