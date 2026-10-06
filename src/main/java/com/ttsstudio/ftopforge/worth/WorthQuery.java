package com.ttsstudio.ftopforge.worth;

import com.ttsstudio.ftopforge.core.ItemPrices;

public final class WorthQuery {

    public static final class Result {
        public final String displayMaterial;
        public final long unit;
        public final long total;
        public final int amount;
        public final boolean notListed;

        public Result(String displayMaterial, long unit, long total, int amount, boolean notListed) {
            this.displayMaterial = displayMaterial; this.unit = unit; this.total = total;
            this.amount = amount; this.notListed = notListed;
        }
    }

    private WorthQuery() {}

    /**
     * Compute worth for a material at the given amount.
     * @param material        material name (e.g. "DIAMOND_BLOCK", "MOB_SPAWNER")
     * @param amount          quantity (>=1)
     * @param spawnerEntityType  if material is "MOB_SPAWNER", the entity type ("PIG", "ZOMBIE", etc.) - else null
     * @param prices          ItemPrices instance
     * @return Result or null if material is null
     */
    public static Result compute(String material, int amount, String spawnerEntityType, ItemPrices prices) {
        if (material == null) return null;
        long unit;
        String display = material;
        if ("MOB_SPAWNER".equalsIgnoreCase(material) && spawnerEntityType != null) {
            unit = prices.spawnerValue(spawnerEntityType);
            display = spawnerEntityType.toUpperCase() + "_SPAWNER";
        } else {
            // Try blockValue first (placement-oriented), then itemValue as fallback
            unit = prices.blockValue(material);
            if (unit == 0L) unit = prices.itemValue(material);
        }
        boolean notListed = unit <= 0L;
        int safeAmount = Math.max(amount, 0);
        long total = unit * safeAmount;
        return new Result(display, unit, total, safeAmount, notListed);
    }
}
