package com.ttsstudio.ftopforge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FTopGuiHolder implements InventoryHolder {

    private final int page;
    private Inventory inventory;

    public FTopGuiHolder(int page) {
        this.page = page;
    }

    public int getPage() { return page; }

    public void setInventory(Inventory inv) { this.inventory = inv; }

    @Override
    public Inventory getInventory() { return inventory; }
}
