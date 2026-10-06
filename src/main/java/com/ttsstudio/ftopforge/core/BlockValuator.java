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
                if (v > 0) configBlocks.put(k.toUpperCase(), v);
            }
        }
    }

    /** Returns the price for this block (0 if not in any value map). */
    public long valueOf(Block b) {
        if (b == null) return 0L;
        Material m = b.getType();
        if (m == null || m == Material.AIR) return 0L;
        String name = m.name();
        // Check both config.yml-style Blocks (Beacon, Hopper) and items.yml#Blocks (DiamondBlock, EmeraldBlock)
        Long cv = configBlocks.get(name);
        if (cv != null) return cv;
        return prices.blockValue(name);
    }
}
