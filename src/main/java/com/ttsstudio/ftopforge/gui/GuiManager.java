package com.ttsstudio.ftopforge.gui;

import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.TopCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuiManager implements Listener {

    private final Plugin plugin;
    private final TopGui topGui;
    private final BreakdownGui breakdownGui;
    private final TopCache topCache;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public GuiManager(Plugin plugin, TopGui topGui, BreakdownGui breakdownGui, TopCache topCache) {
        this.plugin = plugin;
        this.topGui = topGui;
        this.breakdownGui = breakdownGui;
        this.topCache = topCache;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    public void openTop(Player player, int page) {
        topGui.open(player, page);
        openViewers.add(player.getUniqueId());
    }

    public void openBreakdown(Player player, String factionId, int originPage) {
        breakdownGui.open(player, factionId, originPage);
        openViewers.add(player.getUniqueId());
    }

    public void closeAll() {
        Set<UUID> snapshot = new HashSet<UUID>(openViewers);
        for (UUID viewerId : snapshot) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        openViewers.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof FTopGuiHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            FTopGuiHolder topHolder = (FTopGuiHolder) holder;
            int rawSlot = event.getRawSlot();
            if (rawSlot < 0 || rawSlot >= TopGuiLayout.INV_SIZE) return;

            if (rawSlot == TopGuiLayout.SLOT_PREV) {
                if (TopGuiLayout.isPrevVisible(topHolder.getPage())) {
                    openTop(player, topHolder.getPage() - 1);
                }
            } else if (rawSlot == TopGuiLayout.SLOT_CLOSE) {
                player.closeInventory();
            } else if (rawSlot == TopGuiLayout.SLOT_INFO) {
                // info slot — no-op
            } else if (rawSlot == TopGuiLayout.SLOT_NEXT) {
                int totalPages = TopGuiLayout.totalPages(topCache.size());
                if (TopGuiLayout.isNextVisible(topHolder.getPage(), totalPages)) {
                    openTop(player, topHolder.getPage() + 1);
                }
            } else if (rawSlot < TopGuiLayout.PER_PAGE) {
                // skull slot — resolve faction
                int[] range = TopGuiLayout.rangeForPage(topHolder.getPage(), topCache.size());
                List<FactionSnapshot> page = topCache.topRange(range[0], range[1]);
                if (rawSlot < page.size()) {
                    openBreakdown(player, page.get(rawSlot).factionId(), topHolder.getPage());
                }
            }
            return;
        }
        if (holder instanceof FBreakdownGuiHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            FBreakdownGuiHolder bdHolder = (FBreakdownGuiHolder) holder;
            int rawSlot = event.getRawSlot();
            if (rawSlot == 4) {
                player.closeInventory();
            } else if (rawSlot == 22) {
                openTop(player, bdHolder.getOriginPage());
            }
        }
    }

    /** Dragging dropped the player's own items into a menu that is thrown away on close. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof FTopGuiHolder || holder instanceof FBreakdownGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof FTopGuiHolder || holder instanceof FBreakdownGuiHolder) {
            openViewers.remove(event.getPlayer().getUniqueId());
        }
    }
}
