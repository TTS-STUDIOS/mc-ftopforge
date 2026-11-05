package com.ttsstudio.ftopforge.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class BlockValuator {

    private final ItemPrices prices;
    private final Map<String, Long> configBlocks; // from config.yml#Blocks (Beacon/Hopper/etc.)

    public BlockValuator(ItemPrices prices, ConfigurationSection configBlocksSection) {
        this.prices = prices;
        this.configBlocks = new HashMap<>();
        if (configBlocksSection != null) {
            for (String k : configBlocksSection.getKeys(false)) {
                long v = configBlocksSection.getLong(k, 0L);
                if (v > 0) configBlocks.put(k.toUpperCase(java.util.Locale.ROOT), v);
            }
        }
    }

    /** Returns the price for this block (0 if not in any value map). */
    public long valueOf(Block b) {
        return b == null ? 0L : valueOf(b.getType());
    }

    /** Price of one block of this material; config.yml#Blocks wins over items.yml#Blocks. */
    public long valueOf(Material m) {
        if (m == null || m == Material.AIR) return 0L;
        Long cv = configBlocks.get(m.name());
        if (cv != null) return cv;
        return prices.blockValue(m.name());
    }
}
