package com.ttsstudio.ftopforge.gui;

import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.RecalcRunner;
import com.ttsstudio.ftopforge.core.TopCache;
import com.ttsstudio.ftopforge.i18n.Messages;
import com.ttsstudio.ftopforge.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TopGui {

    private final TopCache topCache;
    private final SkullCache skullCache;
    private final Messages msg;
    private final RecalcRunner recalcRunner;
    private final MaterialKey fillerKey;

    public TopGui(TopCache topCache, SkullCache skullCache, Messages msg, RecalcRunner recalcRunner,
                  ConfigurationSection guiSection) {
        this.topCache = topCache;
        this.skullCache = skullCache;
        this.msg = msg;
        this.recalcRunner = recalcRunner;
        String raw = guiSection != null
                ? guiSection.getString("top.filler", "STAINED_GLASS_PANE-7")
                : "STAINED_GLASS_PANE-7";
        this.fillerKey = MaterialKey.parse(raw);
    }

    public void open(Player player, int requestedPage) {
        int total = topCache.size();
        int totalPages = TopGuiLayout.totalPages(total);
        int page = TopGuiLayout.clampPage(requestedPage, totalPages);
        int[] range = TopGuiLayout.rangeForPage(page, total);

        String title = ChatColor.translateAlternateColorCodes('&',
                msg.get("gui.top.title",
                        "page", String.valueOf(page),
                        "total", String.valueOf(totalPages)));
        if (title.length() > 32) title = title.substring(0, 32); // 1.8/1.9 hard limit

        FTopGuiHolder holder = new FTopGuiHolder(page);
        Inventory inv = Bukkit.createInventory(holder, TopGuiLayout.INV_SIZE, title);
        holder.setInventory(inv);

        List<FactionSnapshot> pageList = topCache.topRange(range[0], range[1]);
        for (int i = 0; i < pageList.size(); i++) {
            FactionSnapshot s = pageList.get(i);
            inv.setItem(i, buildSkull(s, range[0] + i + 1));
        }

        ItemStack filler = buildFiller();
        for (int i = pageList.size(); i < TopGuiLayout.PER_PAGE; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(TopGuiLayout.SLOT_PREV,
                TopGuiLayout.isPrevVisible(page)
                        ? simpleNamed("ARROW", (short) 0, msg.get("gui.top.nav.prev"))
                        : filler);
        inv.setItem(46, filler);
        inv.setItem(47, filler);
        inv.setItem(TopGuiLayout.SLOT_CLOSE, simpleNamed("BARRIER", (short) 0, msg.get("gui.top.nav.close")));
        inv.setItem(TopGuiLayout.SLOT_INFO, buildInfoClock(total));
        inv.setItem(50, filler);
        inv.setItem(51, filler);
        inv.setItem(52, filler);
        inv.setItem(TopGuiLayout.SLOT_NEXT,
                TopGuiLayout.isNextVisible(page, totalPages)
                        ? simpleNamed("ARROW", (short) 0, msg.get("gui.top.nav.next"))
                        : filler);

        player.openInventory(inv);
    }

    private ItemStack buildFiller() {
        return simpleNamed(fillerKey.name, fillerKey.data, " ");
    }

    private ItemStack buildSkull(FactionSnapshot s, int rank) {
        UUID leader = s.leaderUuid() != null ? safeUuid(s.leaderUuid()) : null;
        ItemStack stack = leader != null
                ? skullCache.getOrFetch(leader, s.leaderName())
                : new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(msg.get("gui.top.skull.name",
                    "rank", String.valueOf(rank),
                    "faction_name", s.factionName()));
            List<String> lore = new ArrayList<String>();
            lore.add(msg.get("gui.top.skull.lore_leader",
                    "leader_name", s.leaderName() != null ? s.leaderName() : "?"));
            lore.add(msg.get("gui.top.skull.lore_value",
                    "formatted_value", MoneyFormat.shortFmt(s.totalValue())));
            lore.add(" ");
            lore.add(msg.get("gui.top.skull.lore_action"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack buildInfoClock(int total) {
        // 1.8 uses Material.WATCH for the clock
        ItemStack stack = simpleNamed("WATCH", (short) 0, msg.get("gui.top.info.name"));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(msg.get("gui.top.info.lore_last", "relative_time", relativeLast()));
            lore.add(msg.get("gui.top.info.lore_next", "countdown", countdown()));
            lore.add(msg.get("gui.top.info.lore_total", "total", String.valueOf(total)));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String relativeLast() {
        long last = recalcRunner.lastFinishedAt();
        if (last <= 0) return msg.get("gui.top.relative.never");
        return formatDelta(System.currentTimeMillis() - last);
    }

    private String countdown() {
        if (recalcRunner.isRunning()) return msg.get("gui.top.relative.recalculating");
        long next = recalcRunner.nextScheduledAt();
        if (next <= 0) return "?";
        long deltaMs = next - System.currentTimeMillis();
        if (deltaMs < 0) deltaMs = 0;
        return formatDeltaPositive(deltaMs);
    }

    /** Format milliseconds as "hace 3m 12s" (relative past). */
    public static String formatDelta(long ms) {
        long secs = ms / 1000;
        if (secs < 60) return "hace " + secs + "s";
        long mins = secs / 60;
        long remSec = secs % 60;
        if (mins < 60) return "hace " + mins + "m " + remSec + "s";
        long hours = mins / 60;
        long remMin = mins % 60;
        return "hace " + hours + "h " + remMin + "m";
    }

    /** Format milliseconds as "3m 12s" (positive duration, no prefix). */
    public static String formatDeltaPositive(long ms) {
        long secs = ms / 1000;
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        long remSec = secs % 60;
        if (mins < 60) return mins + "m " + remSec + "s";
        long hours = mins / 60;
        long remMin = mins % 60;
        return hours + "h " + remMin + "m";
    }

    private static ItemStack simpleNamed(String materialName, short data, String name) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.STONE;
        @SuppressWarnings("deprecation")
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static UUID safeUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
