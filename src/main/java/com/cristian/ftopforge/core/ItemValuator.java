package com.cristian.ftopforge.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ItemValuator {

    private final ItemPrices prices;

    public ItemValuator(ItemPrices prices) {
        this.prices = prices;
    }

    /** Returns the total value of items inside this block (chest, hopper, dispenser, dropper). 0 for non-containers. */
    public long valueOfContents(Block b) {
        if (b == null) return 0L;
        BlockState st;
        try { st = b.getState(); } catch (Exception e) { return 0L; }
        if (!(st instanceof InventoryHolder)) return 0L;
        // Each half of a double chest is its own tile entity, and getInventory()
        // answers with both halves: the whole chest was counted twice.
        Inventory inv = st instanceof org.bukkit.block.Chest
            ? ((org.bukkit.block.Chest) st).getBlockInventory()
            : ((InventoryHolder) st).getInventory();
        long sum = 0;
        for (ItemStack is : inv.getContents()) {
            if (is == null || is.getType() == null || is.getType() == Material.AIR) continue;
            long per = prices.itemValue(is.getType().name());
            if (per > 0) sum += per * is.getAmount();
        }
        return sum;
    }
}
