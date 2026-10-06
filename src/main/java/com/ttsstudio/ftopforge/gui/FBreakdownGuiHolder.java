package com.ttsstudio.ftopforge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FBreakdownGuiHolder implements InventoryHolder {

    private final String factionId;
    private final int originPage;
    private Inventory inventory;

    public FBreakdownGuiHolder(String factionId, int originPage) {
        this.factionId = factionId;
        this.originPage = originPage;
    }

    public String getFactionId() { return factionId; }
    public int getOriginPage() { return originPage; }

    public void setInventory(Inventory inv) { this.inventory = inv; }

    @Override
    public Inventory getInventory() { return inventory; }
}
